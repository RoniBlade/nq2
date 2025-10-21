package org.example.service.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.v1.entity.EnumValueEntity;
import org.example.v1.entity.ObjectTypeFieldEntity;
import org.example.v1.repository.EnumValueRepository;
import org.example.v1.repository.ObjectTypeFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher; // NEW
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SchemaLoaderService {

    private final WebClient webClient;
    private final ObjectTypeFieldRepository fieldRepo;
    private final EnumValueRepository enumRepo;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SchemaLoaderService(WebClient webClient,
                               ObjectTypeFieldRepository fieldRepo,
                               EnumValueRepository enumRepo,
                               CacheManager cacheManager,
                               ApplicationEventPublisher eventPublisher) { // NEW
        this.webClient = webClient;
        this.fieldRepo = fieldRepo;
        this.enumRepo = enumRepo;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher; // NEW
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void loadSchemaOnStartup() {
        log.info("┌──────────────────────────────────────────────────┐");
        log.info("│ Starting async MidPoint schema load…             │");
        log.info("└──────────────────────────────────────────────────┘");

        try {
            String response = webClient.post()
                    .uri("/api/glt/attributes/all-types")
                    .body(BodyInserters.fromValue(List.of()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                Map<String, Map<String, String>> schema = processSchemaJson(response);
                preloadCache(schema);
                log.info("MidPoint schema loaded successfully.");
            } else {
                log.warn("Empty response from MidPoint API.");
            }
        } catch (Exception ex) {
            log.error("\n╔══════════════════════════════════════════════════╗\n" +
                            "║               SCHEMA LOAD ERROR                  ║\n" +
                            "╠══════════════════════════════════════════════════╣\n" +
                            "║ {}\n" +
                            "╚══════════════════════════════════════════════════╝",
                    ex.getMessage(), ex);
        } finally {
            log.info("┌──────────────────────────────────────────────────┐");
            log.info("│ MidPoint schema load task finished.              │");
            log.info("└──────────────────────────────────────────────────┘");
        }
    }

    private Map<String, Map<String, String>> processSchemaJson(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<ObjectTypeFieldEntity> newFields = new ArrayList<>();
        List<EnumValueEntity> newEnums = new ArrayList<>();
        Map<String, Map<String, String>> schemaMap = new HashMap<>();

        Pattern enumPattern = Pattern.compile("^enum\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
        Pattern parenPattern = Pattern.compile("^([^\\(]+?)\\s*\\(([^\\)]+)\\)$");

        root.fields().forEachRemaining(typeEntry -> {
            // CHANGED: убираем "Type" без .toUpperCase()
            String objectType = stripTypeSuffix(typeEntry.getKey(), "Type").toUpperCase();

            JsonNode valueNode = typeEntry.getValue();

            if (valueNode.isTextual() && "Not found".equals(valueNode.textValue())) {
                log.warn("Type {} not found in MidPoint (\"Not found\")", objectType);
                return;
            }

            Map<String, String> fieldsMap = new HashMap<>();

            if (valueNode.isObject()) {
                valueNode.fields().forEachRemaining(fieldEntry -> {
                    String fieldName = fieldEntry.getKey();
                    JsonNode info = fieldEntry.getValue();

                    String fieldType = parseFieldType(fieldName, info, newEnums);
                    // сохраняем только objectType, fieldName, fieldType
                    newFields.add(new ObjectTypeFieldEntity(objectType, fieldName, fieldType));
                    fieldsMap.put(fieldName, fieldType);
                });
            }
            schemaMap.put(objectType, fieldsMap);
        });

        updateDatabase(newFields, newEnums);
        return schemaMap;
    }

    private String parseFieldType(String fieldName, JsonNode info, List<EnumValueEntity> newEnums) {
        Pattern enumPattern = Pattern.compile("^enum\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
        Pattern parenPattern = Pattern.compile("^([^\\(]+?)\\s*\\(([^\\)]+)\\)$");

        if (info.isTextual()) {
            String text = info.textValue().replaceAll("\\s+", " ").trim();
            Matcher mEnum = enumPattern.matcher(text);
            Matcher mParen = parenPattern.matcher(text);

            if (mEnum.matches()) {
                String enumList = mEnum.group(1);
                String enumType = capitalize(fieldName) + "Type";
                Arrays.stream(enumList.split("\\s*,\\s*"))
                        .forEach(val -> newEnums.add(new EnumValueEntity(enumType, val)));
                return enumType;
            } else if (mParen.matches()) {
                String enumType = mParen.group(1).trim();
                String enumList = mParen.group(2);
                Arrays.stream(enumList.split("\\s*,\\s*"))
                        .forEach(val -> newEnums.add(new EnumValueEntity(enumType, val)));
                return enumType;
            } else {
                return text;
            }
        } else if (info.isArray()) {
            String enumType = capitalize(fieldName) + "Type";
            info.forEach(valNode -> {
                if (valNode.isTextual()) newEnums.add(new EnumValueEntity(enumType, valNode.textValue()));
            });
            return enumType;
        } else {
            return "CompositeType";
        }
    }

    private void preloadCache(Map<String, Map<String, String>> schema) {
        schema.forEach((objectType, fields) -> {
            cacheManager.getCache("schema").put(objectType, fields);
            log.info("Preloaded cache for {}", objectType);
        });
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : s.toUpperCase();
    }

    // NEW: helper – remove suffix "Type" preserving case
    private String stripTypeSuffix(String s, String suffix) {
        if (s == null || suffix == null) return s;
        return s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s;
    }

    @Transactional
    protected void updateDatabase(List<ObjectTypeFieldEntity> newFields, List<EnumValueEntity> newEnums) {
        List<ObjectTypeFieldEntity> oldFields = fieldRepo.findAll();
        List<EnumValueEntity> oldEnums = enumRepo.findAll();

        // Типы объектов, пришедшие из MidPoint
        Set<String> incomingObjectTypes = newFields.stream()
                .map(ObjectTypeFieldEntity::getObjecttype)
                .collect(Collectors.toSet());

        // Карты для быстрого доступа
        Map<String, ObjectTypeFieldEntity> oldFieldByKey = oldFields.stream()
                .collect(Collectors.toMap(f -> f.getObjecttype() + ":" + f.getFieldname(), f -> f, (a, b) -> a));

        Map<String, EnumValueEntity> oldEnumByKey = oldEnums.stream()
                .collect(Collectors.toMap(e -> e.getEnumtype() + ":" + e.getEnumvalue(), e -> e, (a, b) -> a));

        Set<String> newFieldKeys = newFields.stream()
                .map(f -> f.getObjecttype() + ":" + f.getFieldname())
                .collect(Collectors.toSet());

        Set<String> newEnumKeys = newEnums.stream()
                .map(e -> e.getEnumtype() + ":" + e.getEnumvalue())
                .collect(Collectors.toSet());

        // 1) ADD/UPDATE поля (меняем только fieldType у существующих)
        for (ObjectTypeFieldEntity f : newFields) {
            String key = f.getObjecttype() + ":" + f.getFieldname();
            ObjectTypeFieldEntity existing = oldFieldByKey.get(key);
            if (existing == null) {
                fieldRepo.save(f); // только добавление новой записи
                log.debug("Added new field {}.{} type {}", f.getObjecttype(), f.getFieldname(), f.getFieldtype());
            } else if (!Objects.equals(existing.getFieldtype(), f.getFieldtype())) {
                existing.setFieldtype(f.getFieldtype()); // обновляем только тип
                fieldRepo.save(existing);
                log.debug("Updated field {}.{} type to {}", existing.getObjecttype(), existing.getFieldname(), existing.getFieldtype());
            }
        }

        // 2) ADD перечисления (не удаляем)
        for (EnumValueEntity e : newEnums) {
            String key = e.getEnumtype() + ":" + e.getEnumvalue();
            if (!oldEnumByKey.containsKey(key)) {
                enumRepo.save(e);
                log.debug("Added new enum {} value {}", e.getEnumtype(), e.getEnumvalue());
            }
        }

        // 3) SIGNAL «удалено» для полей, которых теперь нет (но НЕ удаляем из БД)
        oldFields.stream()
                .filter(of -> incomingObjectTypes.contains(of.getObjecttype())) // сигналим только по тем типам, что пришли сейчас
                .filter(of -> !newFieldKeys.contains(of.getObjecttype() + ":" + of.getFieldname()))
                .forEach(of -> {
                    log.warn("Field missing in fresh schema (signal only): {}.{}", of.getObjecttype(), of.getFieldname());
                    eventPublisher.publishEvent(new FieldRemovedEvent(this, of.getObjecttype(), of.getFieldname()));
                });

        // 4) SIGNAL «удалено» для enum, которых теперь нет (но НЕ удаляем из БД)
        Set<String> incomingEnumTypes = newEnums.stream()
                .map(EnumValueEntity::getEnumtype)
                .collect(Collectors.toSet());

        oldEnums.stream()
                .filter(oe -> incomingEnumTypes.contains(oe.getEnumtype()))
                .filter(oe -> !newEnumKeys.contains(oe.getEnumtype() + ":" + oe.getEnumvalue()))
                .forEach(oe -> {
                    log.warn("Enum value missing in fresh schema (signal only): {} -> {}", oe.getEnumtype(), oe.getEnumvalue());
                    eventPublisher.publishEvent(new EnumRemovedEvent(this, oe.getEnumtype(), oe.getEnumvalue()));
                });
    }

    // ==== EVENTS (simple Spring events to "send" a deletion-like signal) ====

    public static final class FieldRemovedEvent {
        private final Object source;
        private final String objectType;
        private final String fieldName;

        public FieldRemovedEvent(Object source, String objectType, String fieldName) {
            this.source = source;
            this.objectType = objectType;
            this.fieldName = fieldName;
        }
        public Object getSource() { return source; }
        public String getObjectType() { return objectType; }
        public String getFieldName() { return fieldName; }
        @Override public String toString() { return "FieldRemovedEvent{" + objectType + "." + fieldName + "}"; }
    }

    public static final class EnumRemovedEvent {
        private final Object source;
        private final String enumType;
        private final String enumValue;

        public EnumRemovedEvent(Object source, String enumType, String enumValue) {
            this.source = source;
            this.enumType = enumType;
            this.enumValue = enumValue;
        }
        public Object getSource() { return source; }
        public String getEnumType() { return enumType; }
        public String getEnumValue() { return enumValue; }
        @Override public String toString() { return "EnumRemovedEvent{" + enumType + "=" + enumValue + "}"; }
    }
}
