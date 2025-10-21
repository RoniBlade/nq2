package org.example.v1.service;

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
        String lowerCaseObjectType = objectType.toLowerCase();

        Map<String, Object> objectData = fields.stream()
                .filter(e -> e.containsKey("name") && e.containsKey("value"))
                .collect(Collectors.toMap(
                        e -> e.get("name").toString(),
                        e -> e.get("value")
                ));

        if (structureService.checkStructureExist(objectType)) {
            String oid = midPointClient
                    .createObject(lowerCaseObjectType, objectData, authorizationHeader)
                    .block();

            log.info("Объект типа '{}' создан, oid = {}", objectType, oid);
            return Map.of("oid", oid);
        }

        return Map.of();
    }



    public Mono<String> updateObject(String objectType,
                                     String archetype,
                                     UUID oid,
                                     List<AttributeDelta> deltas,
                                     String authorizationHeader) {
        // Проверка структуры до любых вычислений
        if (!structureService.checkStructureExist(objectType)) {
            return Mono.error(new IllegalArgumentException(
                    "Structure '%s' not found".formatted(objectType)
            ));
        }

        // Преобразование objectType в нижний регистр перед отправкой в API
        String lowerCaseObjectType = objectType.toLowerCase();

        // Формируем список изменений
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

        // Отправляем в MidPoint API с преобразованным типом
        return midPointClient.updateObject(lowerCaseObjectType, oid, body, authorizationHeader);
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

