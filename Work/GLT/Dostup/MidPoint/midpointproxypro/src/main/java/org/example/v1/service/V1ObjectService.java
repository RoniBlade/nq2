package org.example.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.client.MidPointClient;
import org.example.dto.AttributeDelta;
import org.example.v1.entity.ObjectTypeFieldEntity;
import org.example.model.filter.FilterRequest;
import org.example.v1.repository.ObjectTypeFieldRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class V1ObjectService {

    private final MidPointClient midPointClient;
    private final StructureService structureService;

    private final ObjectTypeFieldRepository objectTypeFieldRepository;

    public Map<String, Object> createObject(String objectType,
                                            String archetype,
                                            List<Map<String, Object>> fields,
                                            String authorizationHeader) {
        String camelCaseObjectType = Arrays.stream(objectType.toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining());
        camelCaseObjectType = Character.toLowerCase(camelCaseObjectType.charAt(0)) + camelCaseObjectType.substring(1);

        Map<String, Object> objectData = fields.stream()
                .filter(e -> e.containsKey("name") && e.containsKey("value"))
                .collect(Collectors.toMap(
                        e -> e.get("name").toString(),
                        e -> e.get("value")
                ));

        log.info("Отправка запроса в MidPoint:");
        log.info("  URL: {}", "/ws/rest/" + camelCaseObjectType);
        log.info("  Authorization: {}", authorizationHeader != null && !authorizationHeader.isBlank() ? "present" : "missing");
        log.info("  Archetype: {}", archetype);
        log.info("  Object type: {}", camelCaseObjectType);
        log.info("  Body: {}", objectData);

        String oid = midPointClient
                .createObject(camelCaseObjectType, objectData, authorizationHeader)
                .block();

        log.info("Объект типа '{}' создан, oid = {}", camelCaseObjectType, oid);
        return Map.of("oid", oid);
    }



    public Mono<String> updateObject(String objectType,
                                     String archetype,
                                     UUID oid,
                                     List<AttributeDelta> deltas,
                                     String authorizationHeader) {

        // Проверка структуры (если реально нужно — оставляем, но логируем)
        if (true) {
            log.warn("Попытка обновления несуществующей структуры: {}", objectType);
            return Mono.error(new IllegalArgumentException(
                    "Structure '%s' not found".formatted(objectType)
            ));
        }

        // Преобразуем тип в camelCase
        String camelCaseObjectType = Arrays.stream(objectType.toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining());
        camelCaseObjectType = Character.toLowerCase(camelCaseObjectType.charAt(0)) + camelCaseObjectType.substring(1);

        // Формируем список изменений (itemDelta)
        List<Map<String, Object>> itemDeltaList = deltas.stream()
                .map(delta -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("modificationType", delta.getModification());
                    map.put("path", delta.getName().replace("_", "/"));

                    Object value = delta.getValue();
                    if (value instanceof String) {
                        map.put("value", value);
                    } else if (value instanceof List<?> list && list.stream().allMatch(i -> i instanceof Map)) {
                        map.put("value", list);
                    } else if (value instanceof List<?> list) {
                        map.put("value", list);
                    } else if (value instanceof Map<?, ?> mapValue) {
                        map.put("value", List.of(mapValue));
                    } else {
                        map.put("value", List.of(value));
                    }
                    return map;
                })
                .toList();

        // Формируем тело запроса
        Map<String, Object> body = Map.of(
                "objectModification", Map.of("itemDelta", itemDeltaList)
        );

        // 🔹 Подробное логирование перед запросом
        log.info("Отправка запроса на обновление объекта в MidPoint:");
        log.info("  URL: /ws/rest/{}/{}", camelCaseObjectType, oid);
        log.info("  Authorization: {}", authorizationHeader != null && !authorizationHeader.isBlank() ? "present" : "missing");
        log.info("  Archetype: {}", archetype);
        log.info("  Object type: {}", camelCaseObjectType);
        log.info("  OID: {}", oid);
        log.info("  Deltas: {}", itemDeltaList);

        // Можно дополнительно вывести body как JSON (pretty-print)
        try {
            String prettyJson = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body);
            log.debug("  Request body:\n{}", prettyJson);
        } catch (Exception e) {
            log.debug("  Request body (raw): {}", body);
        }

        // Отправляем запрос в MidPoint
        String finalCamelCaseObjectType = camelCaseObjectType;
        return midPointClient.updateObject(camelCaseObjectType, oid, body, authorizationHeader)
                .doOnSuccess(resp -> log.info("✅ Обновление объекта '{}' (oid={}) завершено успешно.", finalCamelCaseObjectType, oid))
                .doOnError(err -> log.error("❌ Ошибка при обновлении объекта '{}' (oid={}): {}", finalCamelCaseObjectType, oid, err.getMessage()));
    }

    public void deleteObject(String objectType, UUID oid, String authorizationHeader) {
        midPointClient.deleteObject(objectType, oid, authorizationHeader)
                .doOnSuccess(unused -> log.info("Объект типа '{}' с oid='{}' успешно удалён", objectType, oid))
                .doOnError(error -> log.error("Ошибка при удалении объекта типа '{}' с oid='{}': {}", objectType, oid, error.getMessage()))
                .block();
    }

    public Page<Map<String, Object>> getObjectsByObjectType(String objectType, String archetype, FilterRequest request, Pageable pageable) {
        Page<Map<String, Object>> raw = structureService.getDataStructure(objectType, archetype, request.getFilters(), pageable);

        List<Map<String, Object>> trimmed = raw.getContent().stream()
                .map(row -> DtoFieldTrimmer.trimMap(
                        row,
                        request.getFields(),
                        request.getExcludeFields()
                ))
                .toList();

        return new PageImpl<>(trimmed, pageable, raw.getTotalElements());
    }

    public Page<Map<String, Object>> getObjectByFieldsAndArchetype(String fieldName, String archetype, FilterRequest request, Pageable pageable) {


        ObjectTypeFieldEntity objectTypeFieldEntity = objectTypeFieldRepository.findByFieldnameAndArchetypeAndSendTrue(fieldName, archetype);

        Page<Map<String, Object>> raw = null;
        if(objectTypeFieldEntity.getExttype().equals("Ref") || objectTypeFieldEntity.getExtobject().equals("Link")) {
            raw =  structureService.getDataStructure(objectTypeFieldEntity.getExtobject(), archetype, request.getFilters(), objectTypeFieldEntity.getExtwhereclause(), pageable);
        }


        List<Map<String, Object>> trimmed = raw.getContent().stream()
                .map(row -> DtoFieldTrimmer.trimMap(
                        row,
                        request.getFields(),
                        request.getExcludeFields()
                ))
                .toList();

        return new PageImpl<>(trimmed, pageable, raw.getTotalElements());


    }
}

