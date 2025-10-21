// src/main/java/org/example/v1/controller/ObjectFieldDefinitionsController.java
package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.dto.ObjectTypeFieldDto;
import org.example.v1.service.ObjectFieldDefinitionsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/object-fields")
@OpenAPIDefinition(info = @Info(title = "Object Field Definitions API (d_object_fields)", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(
        name = "ObjectFieldController",
        description = "CRUD по таблице d_object_fields (администрирование схемы полей)"
)
public class ObjectFieldDefinitionsController {

    private final ObjectFieldDefinitionsService service;

    /* ===================== READ (list & search) ===================== */

    @Operation(
            summary = "Список определений полей (pageable)",
            description = "Возвращает страницу записей d_object_fields. Поддерживает параметры page/size/sort."
    )
    @GetMapping
    public Page<ObjectTypeFieldDto> list(Pageable pageable) {
        return service.getConfigParams(pageable);
    }

    @Operation(
            summary = "Поиск определений полей",
            description = "Ищет в d_object_fields по FilterRequest (filters / fields / excludeFields). Возвращает страницу."
    )
    @PostMapping("/filtered")
    public Page<ObjectTypeFieldDto> search(@RequestBody(required = false) FilterRequest request,
                                           Pageable pageable) {
        // защитимся от null, т.к. в сервисе используется request.getFilters()
        if (request == null) request = new FilterRequest();
        return service.searchConfigParams(request, pageable);
    }

    /* ===================== CREATE / UPDATE / DELETE ===================== */

    @Operation(
            summary = "Создать определение поля (d_object_fields)",
            description = "Административная операция: добавляет mapping/мету для objectType/archetype."
    )
    @PostMapping
    public ResponseEntity<ObjectTypeFieldDto> createObjectTypeField(@RequestBody ObjectTypeFieldDto dto) {
        log.info("[CTRL] POST /object-fields/definitions body={}", dto);
        return ResponseEntity.ok(service.create(dto));
    }

    @Operation(
            summary = "Обновить определение поля (d_object_fields)",
            description = "Административная операция: изменяет существующую запись по id."
    )
    @PutMapping("/{oid}")
    public ResponseEntity<ObjectTypeFieldDto> updateObjectTypeField(@PathVariable UUID oid,
                                                                    @RequestBody ObjectTypeFieldDto dto) {
        log.info("[CTRL] PUT /object-fields/definitions/{} body={}", oid, dto);
        try {
            return ResponseEntity.ok(service.update(oid, dto));
        } catch (EntityNotFoundException e) {
            log.warn("ObjectTypeField not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Удалить определение поля (d_object_fields)",
            description = "Административная операция: удаляет запись по id."
    )
    @DeleteMapping("/{oid}")
    public ResponseEntity<?> deleteObjectTypeField(@PathVariable UUID oid) {
        log.info("[CTRL] DELETE /object-fields/definitions/{}", oid);
        try {
            service.delete(oid);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.warn("ObjectTypeField not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "ObjectTypeField not found", "oid", oid));
        }
    }

    @Operation(
            summary = "Список типов objecttype",
            description = "Возвращает список уникальных значений поля objecttype из d_object_fields"
    )
    @GetMapping("/types")
    public ResponseEntity<List<String>> getObjectTypes() {
        return ResponseEntity.ok(service.getAllObjectTypes());
    }

    @Operation(
            summary = "Список архетипов по objecttype",
            description = "Возвращает список уникальных значений поля archetype для заданного objecttype"
    )
    @GetMapping("/{type}/archetypes")
    public ResponseEntity<List<String>> getArchetypesByType(@PathVariable("type") String objectType) {
        return ResponseEntity.ok(service.getArchetypesByObjectType(objectType));
    }

}
