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
import org.example.v1.dto.EnumValueDto;
import org.example.v1.service.EnumValueDefinitionsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/enum-values")
@OpenAPIDefinition(info = @Info(title = "Enum Value Definitions API (d_enum_values)", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(
        name = "EnumValueController",
        description = "CRUD и поиск по таблице d_enum_values (администрирование значений справочников)"
)
public class EnumValueDefinitionsController {

    private final EnumValueDefinitionsService service;

    /* ===================== READ (list & search) ===================== */

    @Operation(summary = "Список значений enum (pageable)",
            description = "Возвращает страницу записей d_enum_values. Поддерживает page/size/sort.")
    @GetMapping
    public Page<EnumValueDto> list(Pageable pageable) {
        return service.getConfigParams(pageable);
    }

    @Operation(summary = "Поиск значений enum",
            description = "Ищет в d_enum_values по FilterRequest (filters / fields / excludeFields). Возвращает страницу.")
    @PostMapping("/filtered")
    public Page<EnumValueDto> search(@RequestBody(required = false) FilterRequest request,
                                     Pageable pageable) {
        if (request == null) request = new FilterRequest();
        return service.searchConfigParams(request, pageable);
    }

    /* ===================== CREATE / UPDATE / DELETE ===================== */

    @Operation(summary = "Создать значение enum (d_enum_values)",
            description = "Административная операция: добавляет значение для enumType.")
    @PostMapping
    public ResponseEntity<EnumValueDto> create(@RequestBody EnumValueDto dto) {
        log.info("[CTRL] POST /enum-values/definitions body={}", dto);
        return ResponseEntity.ok(service.create(dto));
    }

    @Operation(summary = "Обновить значение enum (d_enum_values)",
            description = "Административная операция: изменяет существующую запись по oid.")
    @PutMapping("/{oid}")
    public ResponseEntity<?> update(@PathVariable UUID oid, @RequestBody EnumValueDto dto) {
        log.info("[CTRL] PUT /enum-values/definitions/{} body={}", oid, dto);
        try {
            return ResponseEntity.ok(service.updateEnumValue(oid, dto));
        } catch (EntityNotFoundException e) {
            log.warn("EnumValue not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "EnumValueField not found", "oid", oid));
        }
    }

    @Operation(summary = "Удалить значение enum (d_enum_values)",
            description = "Административная операция: удаляет запись по oid.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID oid) {
        log.info("[CTRL] DELETE /enum-values/definitions/{}", oid);
        try {
            service.delete(oid);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.warn("EnumValue not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
