package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AttributeDelta;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1ObjectFieldService;
import org.example.v1.service.V1ObjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/object")
@OpenAPIDefinition(info = @Info(title = "Object API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ObjectController", description = "Получение, создание и удаление объектов objectType")
public class V1ObjectController {

    private final V1ObjectService v1ObjectService;

    /**
     * Создаёт новый объект в MidPoint.
     * @param objectType тип объекта (например, user, role, org)
     * @param fields список пар "name" и "value"
     * @param authorizationHeader авторизация (Basic auth)
     * @return Map с OID созданного объекта
     */
    @Operation(summary = "Создание нового объекта в Midpoint", description = "типы создаваемых объектов: user, role, org")
    @PostMapping("/{objectType}")
    public ResponseEntity<Map<String, Object>> createObject(
            @RequestParam(required = false) String archetype,
            @PathVariable String objectType,
            @RequestBody List<Map<String, Object>> fields,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("Запрос на создание объекта: {}", objectType);
        UUID oid = UUID.fromString(v1ObjectService.createObject(objectType, archetype, fields, authorizationHeader).get("oid").toString());

        return valuesByObjectTypeOrArchetypeAndOid(objectType, archetype, oid, new FilterRequest());
    }

    /**
     * Обновляет существующий объект MidPoint (частично).
     * @param objectType тип объекта
     * @param oid идентификатор объекта
     * @param updates список изменений (модификаций)
     * @param authorizationHeader авторизация
     * @return Mono с результатом
     */

    @Operation(summary = "Обновление объекта в Midpoint", description = "типы обновляемых объектов: user, role, org")
    @PutMapping("/{objectType}/{oid}")
    public Mono<ResponseEntity<String>> updateObject(
            @RequestParam String archetype,
            @PathVariable String objectType,
            @PathVariable UUID oid,
            @RequestBody List<AttributeDelta> updates,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("Запрос на обновление {} с oid={}", objectType, oid);
        return v1ObjectService.updateObject(objectType, archetype, oid, updates, authorizationHeader)
                .map(ResponseEntity::ok);
    }


    @Operation(summary = "Удаление объекта в Midpoint", description = "Удаляет объект по oid. Типы: user, role, org")
    @DeleteMapping("/{objectType}/{oid}")
    public ResponseEntity<Void> deleteObject(
            @PathVariable String objectType,
            @PathVariable UUID oid,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("Запрос на удаление объекта: type={}, oid={}", objectType, oid);
        v1ObjectService.deleteObject(objectType, oid, authorizationHeader);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Получение полей по archetype с фильтрацией")
    @PostMapping("/getObjects/{objectType}/{archetype}")
    public Page<Map<String, Object>> getFieldsByArchetype(
            @PathVariable String archetype,
            @PathVariable String objectType,
            @RequestBody(required = false) FilterRequest request,
            Pageable pageable
    ) {
        return v1ObjectService.getObjectsByObjectType(objectType, archetype, request, pageable);
    }

    @Operation(summary = "Получение полей объекта по полю и архетипу", description = "Возвращает объект полученный по определенным условиям")
    @PostMapping("/getObject/{archetype}/field/{fieldName}")
    public Page<Map<String, Object>> getObjectByFieldsAndArchetype(
            @PathVariable String fieldName,
            @PathVariable String archetype,
            @RequestBody(required = false) FilterRequest request,
            Pageable pageable
    ) {
        return v1ObjectService.getObjectByFieldsAndArchetype(fieldName, archetype, request, pageable);
    }


    private final V1ObjectFieldService service;

    /* ===================== META ===================== */

    // POST /getObjectFields/{objectType}
    @Operation(
            summary = "Мета по objectType",
            description = "Возвращает описание колонок для базового objectType (ext_archetype IS NULL/blank). " +
                    "oid/displayname=null. Фильтрация колонок через FilterRequest.fields / excludeFields (опционально)."
    )
    @PostMapping("/getObjectFields/{objectType}")
    public ResponseEntity<Map<String, Object>> metaByObjectType(
            @PathVariable String objectType,
            @RequestBody(required = false) FilterRequest request
    ) {
        return ResponseEntity.ok(service.metaByObjectType(objectType, request));
    }

    // POST /getObjectFields/{objectType}/{archetype}
    @Operation(
            summary = "Мета по archetype (fallback на objectType)",
            description = "Сначала берутся поля с ext_archetype={archetype}; если нет — базовые поля objectType. " +
                    "Фильтрация колонок через FilterRequest.fields / excludeFields (опционально)."
    )
    @PostMapping("/getObjectFields/{objectType}/{archetype}")
    public ResponseEntity<Map<String, Object>> metaByObjectTypeOrArchetype(
            @PathVariable String objectType,
            @PathVariable String archetype,
            @RequestBody(required = false) FilterRequest request
    ) {
        return ResponseEntity.ok(service.metaByObjectTypeOrArchetype(objectType, archetype, request));
    }

    /* ===================== VALUES (ONE) ===================== */

    // POST /getObjectFields/{objectType}/oid/{oid}
    @Operation(
            summary = "Значения по objectType+oid",
            description = "Вызывает функцию «одного объекта» (2 параметра) через StructureService.getObject(...). " +
                    "Возвращает колонки + values/displayvalue. Фильтрация колонок через FilterRequest.fields / excludeFields (опционально)."
    )
    @PostMapping("/getObjectFields/{objectType}/oid/{oid}")
    public ResponseEntity<Map<String, Object>> valuesByObjectTypeAndOid(
            @PathVariable String objectType,
            @PathVariable UUID oid,
            @RequestBody(required = false) FilterRequest request
    ) {
        return ResponseEntity.ok(service.valuesByObjectTypeAndOid(objectType, oid, request));
    }

    // POST /getObjectFields/{objectType}/{archetype}/oid/{oid}
    @Operation(
            summary = "Значения по archetype+oid (fallback на objectType)",
            description = "Если для archetype есть маппинги в d_object_fields — используем их, иначе базовый objectType. " +
                    "Вызывает функцию «одного объекта» (2 параметра). Фильтрация колонок — опционально."
    )
    @PostMapping("/getObjectFields/{objectType}/{archetype}/oid/{oid}")
    public ResponseEntity<Map<String, Object>> valuesByObjectTypeOrArchetypeAndOid(
            @PathVariable String objectType,
            @PathVariable String archetype,
            @PathVariable UUID oid,
            @RequestBody(required = false) FilterRequest request
    ) {
        return ResponseEntity.ok(service.valuesByObjectTypeOrArchetypeAndOid(objectType, archetype, oid, request));
    }
}

