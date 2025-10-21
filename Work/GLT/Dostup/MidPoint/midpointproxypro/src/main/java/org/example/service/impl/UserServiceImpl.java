package org.example.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.example.client.MidPointClient;
import org.example.dto.UserFieldDto;
import org.example.dto.view.UserProfileDto;
import org.example.dto.view.UserProfileExtDto;
import org.example.v1.entity.EnumValueEntity;
import org.example.entity.ObjectArchetypeFieldEntity;
import org.example.v1.entity.ObjectTypeFieldEntity;
import org.example.entity.view.UserProfileEntity;
import org.example.entity.view.UserProfileExtEntity;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterOperation;
import org.example.repository.*;
import org.example.v1.repository.EnumValueRepository;
import org.example.repository.hibernate.ObjectArchetypeFieldRepository;
import org.example.v1.repository.ObjectTypeFieldRepository;
import org.example.repository.hibernate.UserRepository;
import org.example.repository.jdbc.UserProfileExtJdbcRepository;
import org.example.repository.jdbc.UserProfileJdbcRepository;
import org.example.service.UserService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired private ObjectTypeFieldRepository objectFieldRepo;
    @Autowired private ObjectArchetypeFieldRepository objectArchetypeFieldRepo;
    @Autowired private EnumValueRepository enumRepo;
    @Autowired private RefValueRepository refValueRepo;
    @Autowired private MidPointClient midPointClient;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileJdbcRepository userProfileJdbcRepository;
    @Autowired private UserProfileExtJdbcRepository userProfileExtJdbcRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int REQUIRED_FILE_SIZE = 20 * 8 * 1024; // 20 kB

    record SortField(String field, boolean descending) {}

    public Map<String, Object> getUserFieldTemplates(String sortParam, String archetype) {
        log.info("Вызов getUserFieldTemplates(sort={})", sortParam);
        List<ObjectTypeFieldEntity> objectFields = getUserFieldsByArchetypeOrDefault(archetype);
        List<UserFieldDto> result = toListUserFieldDto(objectFields, null);
        applySorting(sortParam, result, UserFieldDto.class);
        return buildResultMap(null, null, result);
    }


    public Map<String, Object> getUserFields(UUID oid, String sortParam, String archetype) {
        log.info("Вызов getUserFields(oid={}, sort={})", oid, sortParam);

        List<FilterNode> filters = List.of(new FilterNode("oid", FilterOperation.EQUAL, oid.toString()));
        Page<UserProfileDto> userPage = userProfileJdbcRepository.findAll(Pageable.unpaged(), filters);

        if (userPage.isEmpty()) {
            throw new IllegalArgumentException("User not found for oid=" + oid);
        }

        UserProfileDto userProfile = userPage.getContent().get(0);

        // 🔽 Получаем ext-атрибуты через JDBC
        List<FilterNode> extFilters = List.of(new FilterNode("oid", FilterOperation.EQUAL, oid.toString()));
        Page<UserProfileExtDto> extPage = userProfileExtJdbcRepository.findAll(Pageable.unpaged(), extFilters);
        Map<String, String> extValueMap = extPage.getContent().stream()
                .collect(Collectors.toMap(UserProfileExtDto::getExtAttrName, UserProfileExtDto::getExtAttrValue));

        Map<String, Field> fieldMap = extractEntityFields(UserProfileDto.class);
        List<ObjectTypeFieldEntity> objectFields = getUserFieldsByArchetypeOrDefault(archetype);

        List<UserFieldDto> result = objectFields.stream()
                .map(field -> {
                    Object value = extractResolvedValue(field.getTablefield(), field.getFieldtype(), userProfile, extValueMap, fieldMap);
                    UserFieldDto dto = toUserFieldDto(field, value);
                    setDisplayNameForReferenceField(dto, field, value);
                    return dto;
                })
                .collect(Collectors.toList());

        applySorting(sortParam, result, UserFieldDto.class);

        return buildResultMap(oid, userProfile.getVDisplayName(), result);
    }

    List<ObjectTypeFieldEntity> getUserFieldsByArchetypeOrDefault(String archetype) {
        if (archetype != null && !archetype.isBlank() && !archetype.equals("default")) {
            List<ObjectArchetypeFieldEntity> archetypeFields =
                    objectArchetypeFieldRepo.findByExtArchetypeAndObjectTypeAndSend(archetype, "USER", true);
            return archetypeFields.stream()
                    .map(this::mapToObjectFieldEntity)
                    .collect(Collectors.toList());
        }
        return objectFieldRepo.findByObjecttypeAndSend("USER", true);
    }


    private ObjectTypeFieldEntity mapToObjectFieldEntity(ObjectArchetypeFieldEntity source) {
        ObjectTypeFieldEntity target = new ObjectTypeFieldEntity();
        target.setFieldname(source.getFieldName());
        target.setFieldtype(source.getFieldType());
        target.setObjecttype(source.getObjectType());
        target.setTablefield(source.getTableField());
        target.setSend(source.getSend());
        target.setVisible(source.getVisible());
        target.setRead(source.getRead());
        target.setTabname(source.getTabName());
        target.setExtorder(source.getExtOrder());
        target.setExttype(source.getExtType());
        target.setExtobject(source.getExtObject());
        target.setExtwhereclause(source.getExtWhereclause());
        target.setExtnotes(source.getExtNotes());
        return target;
    }



    private void setDisplayNameForReferenceField(UserFieldDto dto, ObjectTypeFieldEntity field, Object value) {
        if ("ref".equalsIgnoreCase(field.getExttype()) && value != null) {
            String displayName = refValueRepo.getDisplayName(value.toString(), field.getExtobject());
            dto.setDisplayNameValue(displayName);
        }
        if ("link".equalsIgnoreCase(field.getExttype()) && value != null) {
            String displayName = refValueRepo.getDisplayName(value.toString(), field.getExtobject());
            dto.setDisplayNameValue(displayName);
        }
    }

    private <T> Map<String, Field> extractEntityFields(Class<T> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .collect(Collectors.toMap(f -> f.getAnnotation(Column.class).name(), f -> f));
    }

    private Map<String, String> extractExtValues(List<UserProfileExtEntity> extEntities) {
        return extEntities.stream()
                .filter(e -> e.getExtAttrName() != null)
                .collect(Collectors.toMap(UserProfileExtEntity::getExtAttrName, UserProfileExtEntity::getExtAttrValue));
    }

    private <T> void applySorting(String sortParam, List<T> result, Class<T> clazz) {
        SortField sortField = parseSortField(sortParam);
        if (sortField != null) {
            sortListByFields(result, List.of(sortField), clazz);
        }
    }

    private Map<String, Object> buildResultMap(UUID oid, String displayName, List<UserFieldDto> dtoList) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("oid", oid);
        resultMap.put("vDisplayName", displayName);
        resultMap.put("columns", dtoList);
        return resultMap;
    }

    private List<UserFieldDto> toListUserFieldDto(List<ObjectTypeFieldEntity> objectFields, UserProfileEntity userProfile) {
        return objectFields.stream()
                .map(f -> toUserFieldDto(f, null))
                .collect(Collectors.toList());
    }

    private UserFieldDto toUserFieldDto(ObjectTypeFieldEntity objectTypeFieldEntity, Object value) {
        return new UserFieldDto(
                objectTypeFieldEntity.getFieldname(),
                objectTypeFieldEntity.getFieldtype(),
                value,
                objectTypeFieldEntity.getObjecttype(),
                objectTypeFieldEntity.getTablefield(),
                objectTypeFieldEntity.getVisible(),
                objectTypeFieldEntity.getRead(),
                objectTypeFieldEntity.getTabname(),
                objectTypeFieldEntity.getExtorder(),
                objectTypeFieldEntity.getExttype(),
                objectTypeFieldEntity.getExtobject(),
                objectTypeFieldEntity.getExtwhereclause(),
                objectTypeFieldEntity.getExtnotes(),
                getEnumValues(objectTypeFieldEntity.getFieldname(), objectTypeFieldEntity.getExtobject()));
    }

    Object extractResolvedValue(String tableField, String fieldType,
                                UserProfileEntity userProfile, Map<String, String> extValues,
                                Map<String, Field> fieldMap) {
        if (tableField == null || tableField.isBlank()) return null;
        if (extValues.containsKey(tableField)) {
            String raw = extValues.get(tableField);
            return raw != null ? parseValue(fieldType, raw) : null;
        }
        if (userProfile == null) return null;
        Field field = fieldMap.get(tableField);
        if (field == null) return null;
        try {
            field.setAccessible(true);
            Object value = field.get(userProfile);
            if (value == null) return null;
            if (value instanceof byte[] bytes) {
                return Base64.getEncoder().encodeToString(bytes);
            }
            return parseValue(fieldType, value.toString());
        } catch (IllegalAccessException e) {
            log.error("Ошибка доступа к полю {}: {}", tableField, e.getMessage());
            return null;
        }
    }

    Object extractResolvedValue(String tableField, String fieldType,
                                Object userDtoOrEntity,
                                Map<String, String> extValues,
                                Map<String, Field> fieldMap) {
        if (tableField == null || tableField.isBlank()) return null;

        // 🔽 ext-значения
        if (extValues.containsKey(tableField)) {
            String raw = extValues.get(tableField);
            return raw != null ? parseValue(fieldType, raw) : null;
        }

        if (userDtoOrEntity == null) return null;

        try {
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(userDtoOrEntity);
            Object value = wrapper.getPropertyValue(tableField);

            if (value == null) return null;
            if (value instanceof byte[] bytes) {
                return Base64.getEncoder().encodeToString(bytes);
            }
            return parseValue(fieldType, value.toString());

        } catch (Exception e) {
            log.warn("Не удалось получить поле '{}' через BeanWrapper: {}", tableField, e.getMessage());
            return null;
        }
    }


    SortField parseSortField(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) return null;
        String[] parts = sortParam.split(",");
        return new SortField(parts[0].trim(), parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()));
    }

    <T> void sortListByFields(List<T> list, List<SortField> sortFields, Class<T> clazz) {
        Comparator<T> combined = null;
        for (SortField sortField : sortFields) {
            try {
                Field targetField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(Column.class) && sortField.field().equals(f.getAnnotation(Column.class).name()))
                        .findFirst()
                        .orElse(clazz.getDeclaredField(sortField.field()));

                targetField.setAccessible(true);

                Comparator<T> next = Comparator.comparing(
                        (T obj) -> {
                            try {
                                Object val = targetField.get(obj);
                                return val == null ? "" : val.toString();
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        Comparator.nullsLast(String::compareTo)
                );

                if (sortField.descending()) next = next.reversed();
                combined = combined == null ? next : combined.thenComparing(next);
            } catch (Exception e) {
                throw new IllegalArgumentException("Ошибка при сортировке по полю: " + sortField.field(), e);
            }
        }
        if (combined != null) list.sort(combined);
    }

    Object parseValue(String type, String rawValue) {
        if (type != null && type.startsWith("List<PolyStringType>")) {
            try {
                List<Map<String, String>> parsed = objectMapper.readValue(rawValue, new TypeReference<>() {});
                return parsed.stream().map(entry -> entry.get("o")).filter(Objects::nonNull).collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Не удалось распарсить List<PolyStringType>: {}", e.getMessage());
                return Collections.emptyList();
            }
        }
        return rawValue;
    }

    List<String> getEnumValues(String fieldName, String enumType) {
        if (fieldName == null) return null;
        return switch (fieldName.toLowerCase()) {
            case "locale" -> List.of("cs", "de", "et", "en", "es", "fi", "fr", "hu", "it", "ja", "lt", "pl", "pt_BR", "ru", "sk", "tr", "zh_CN");
            case "preferredlanguage" -> List.of("Čeština", "Deutsch", "Eesti", "English", "Español", "Suomi",
                    "Français", "Magyar", "Italiano", "日本語", "Lietuviškai",
                    "Polski", "Português (Brasil)", "Русский", "Slovenčina",
                    "Türkçe", "中文");
            default -> (enumType == null || enumType.isBlank()) ? null :
                    enumRepo.findByEnumtype(enumType).stream().map(EnumValueEntity::getEnumvalue).collect(Collectors.toList());
        };
    }

    public ResponseEntity<Object> postPhoto(MultipartFile file, UUID oid) throws IOException {
        byte[] imageByte = file.getSize() > REQUIRED_FILE_SIZE ? compressPhoto(file) : file.getBytes();
        userRepository.updatePhotoByOid(imageByte, oid);
        return ResponseEntity.accepted().body("Фотография загружена");
    }

    public byte[] getPhoto(UUID oid) {
        return userRepository.getPhotoByOid(oid);
    }

    public ResponseEntity<Object> deletePhoto(UUID oid) {
        userRepository.deletePhotoByOid(oid);
        return ResponseEntity.accepted().body("Фотография удалена");
    }

    public byte[] compressPhoto(MultipartFile file) throws IOException {
        float quality = 0.2f;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .scale(quality)
                .outputQuality(quality)
                .toOutputStream(baos);

        byte[] compressedImageBytes = baos.toByteArray();
        log.info("file size is = {}", compressedImageBytes.length);

        while (compressedImageBytes.length > REQUIRED_FILE_SIZE) {
            if (quality <= 0.1f) return compressedImageBytes;
            baos.reset();
            Thumbnails.of(file.getInputStream())
                    .scale(quality)
                    .outputQuality(quality)
                    .toOutputStream(baos);
            compressedImageBytes = baos.toByteArray();
            quality *= quality;
            log.info("file size is = {}", compressedImageBytes.length / 8 / 1024);
        }
        return compressedImageBytes;
    }
}
