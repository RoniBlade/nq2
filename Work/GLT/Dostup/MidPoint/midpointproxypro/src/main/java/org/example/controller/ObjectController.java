//package org.example.controller;
//
//import io.swagger.v3.oas.annotations.OpenAPIDefinition;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.info.Info;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.dto.AttributeDelta;
//import org.example.model.filter.FilterRequest;
//import org.example.service.ObjectService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/object")
//@OpenAPIDefinition(info = @Info(title = "Create & update object API", version = "v1"))
//@SecurityRequirement(name = "basicAuth")
//@RequiredArgsConstructor
//@Slf4j
//public class ObjectController {
//
//    private final ObjectService objectService;
//
//    /**
//     * Создаёт новый объект в MidPoint.
//     * @param objectType тип объекта (например, user, role, org)
//     * @param fields список пар "name" и "value"
//     * @param authorizationHeader авторизация (Basic auth)
//     * @return Map с OID созданного объекта
//     */
//
//    @Operation(summary = "Создание нового объекта в Midpoint", description = "типы создаваемых объектов: user, role, org")
//    @PostMapping("/{objectType}")
//    public Map<String, Object> createObject(
//            @PathVariable String objectType,
//            @RequestBody List<Map<String, Object>> fields,
//            @RequestHeader("Authorization") String authorizationHeader
//    ) {
//        log.info("Запрос на создание объекта: {}", objectType);
//        return objectService.createObject(objectType, fields, authorizationHeader);
//    }
//
//    /**
//     * Обновляет существующий объект MidPoint (частично).
//     * @param objectType тип объекта
//     * @param oid идентификатор объекта
//     * @param updates список изменений (модификаций)
//     * @param authorizationHeader авторизация
//     * @return Mono с результатом
//     */
//
//    @Operation(summary = "Обновление объекта в Midpoint", description = "типы обновляемых объектов: user, role, org")
//    @PutMapping("/{objectType}/{oid}")
//    public Mono<ResponseEntity<String>> updateObject(
//            @PathVariable String objectType,
//            @PathVariable UUID oid,
//            @RequestBody List<AttributeDelta> updates,
//            @RequestHeader("Authorization") String authorizationHeader
//    ) {
//        log.info("🔄 Запрос на обновление {} с oid={}", objectType, oid);
//        return objectService.updateObject(objectType, oid, updates, authorizationHeader)
//                .map(ResponseEntity::ok);
//    }
//
//
//    @Operation(summary = "Удаление объекта в Midpoint", description = "Удаляет объект по oid. Типы: user, role, org")
//    @DeleteMapping("/{objectType}/{oid}")
//    public ResponseEntity<Void> deleteObject(
//            @PathVariable String objectType,
//            @PathVariable UUID oid,
//            @RequestHeader("Authorization") String authorizationHeader
//    ) {
//        log.info("🗑️ Запрос на удаление объекта: type={}, oid={}", objectType, oid);
//        objectService.deleteObject(objectType, oid, authorizationHeader);
//        return ResponseEntity.noContent().build();
//    }
//
//
//    @Operation(summary = "Получение полей по archetype без фильтрации")
//    @GetMapping("/getObjectFields/{archetype}")
//    public Map<String, Object>  getFieldsByArchetype(
//            @PathVariable String archetype,
//            Pageable pageable
//    ) {
//        return objectService.getFieldsByArchetypeOrByObjectType(archetype, pageable);
//    }
//
//    @Operation(summary = "Получение полей по archetype с фильтрацией")
//    @PostMapping("/getObjectFields/{archetype}")
//    public  Map<String, Object> searchFieldsByArchetypeAndFieldName(
//            @PathVariable String archetype,
//            @RequestBody(required = false) FilterRequest request,
//            Pageable pageable
//    ) {
//        return objectService.searchFieldsByArchetype(archetype, request, pageable);
//    }
//
//    @Operation(summary = "Получение значений полей по archetype и oid с фильтрацией")
//    @PostMapping("/getObjectFields/{archetype}/{oid}")
//    public ResponseEntity<Map<String, Object>> getObjectValues(
//            @PathVariable String archetype,
//            @PathVariable UUID oid,
//            @RequestBody(required = false) FilterRequest request,
//            Pageable pageable
//    ) {
//        Map<String, Object> result = objectService.getFieldsByArchetypeAndOid(archetype, oid, request, pageable);
//        return ResponseEntity.ok(result);
//    }
//
//    @Operation(summary = "Получение данных по ext_object и ext_whereclause для поля")
//    @PostMapping("/getObject/{archetype}/field/{fieldName}")
//    public Page<Map<String, Object>>  getByFieldName(
//            @PathVariable String archetype,
//            @PathVariable String fieldName,
//            @RequestBody(required = false) FilterRequest request,
//            Pageable pageable
//    ) {
//        return objectService.getByFieldNameAndArchetypeWithExtQuery(archetype, fieldName, request, pageable);
//    }
//
//    @Operation(summary = "Получение полей по objectType без фильтрации")
//    @GetMapping("/getObjects/{objectType}")
//    public  Page<?> getFieldsByObjectType(
//            @PathVariable String objectType,
//            Pageable pageable
//    ) {
//        return objectService.getObjectsByObjectType(objectType, pageable);
//    }
//
//    @Operation(summary = "Получение полей по objectType с фильтрацией")
//    @PostMapping("/getObjects/{objectType}")
//    public Page<?> searchFieldsByObjectType(
//            @PathVariable String objectType,
//            @RequestBody(required = false) FilterRequest request,
//            Pageable pageable
//    ) {
//        return objectService.searchObjectsByObjectType(objectType, request, pageable);
//    }
//
//    @Operation(summary = "Получение полей объекта по archetype и oid", description = "Возвращает поля с подставленными значениями")
//    @PostMapping("/getObjectFields/{objectType}/{archetype}/{oid}")
//    public Map<String, Object> getFieldsByArchetypeAndOid(
//            @PathVariable String objectType,
//            @PathVariable String archetype,
//            @PathVariable UUID oid,
//            @RequestBody(required = false) FilterRequest request,
//            Pageable pageable
//    ) {
//        return objectService.getFieldsByObjectTypeAndArchetypeAndOid(objectType, archetype, oid, request, pageable);
//    }
//
//}