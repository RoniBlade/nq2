//package org.example.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.client.MidPointClient;
//import org.example.dto.AttributeDelta;
//import org.example.dto.ObjectArchetypeFieldDto;
//import org.example.v1.dto.ObjectTypeFieldDto;
//import org.example.v1.entity.EnumValueEntity;
//import org.example.entity.ObjectArchetypeFieldEntity;
//import org.example.v1.entity.ObjectTypeFieldEntity;
//import org.example.mapper.CaseMapper;
//import org.example.mapper.ObjectArchetypeFieldMapper;
//import org.example.v1.mapper.ObjectTypeFieldMapper;
//import org.example.mapper.ObjectTypeToArchetypeMapper;
//import org.example.model.filter.FilterNode;
//import org.example.model.filter.FilterOperation;
//import org.example.model.filter.FilterRequest;
//import org.example.repository.*;
//import org.example.repository.hibernate.CaseRepository;
//import org.example.v1.repository.EnumValueRepository;
//import org.example.repository.hibernate.ObjectArchetypeFieldRepository;
//import org.example.v1.repository.ObjectTypeFieldRepository;
//import org.example.repository.jdbc.UserProfileJdbcRepository;
//import org.example.util.RepositoryResolver;
//import org.example.util.field.DtoFieldTrimmer;
//import org.example.util.filter.FilterSpecificationBuilder;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ObjectService {
//
//    private final MidPointClient midPointClient;
//    private final ObjectArchetypeFieldRepository objectArchetypeFieldRepository;
//    private final ObjectTypeFieldRepository objectTypeFieldRepository;
//
//    private final ObjectArchetypeFieldMapper objectArchetypeFieldMapper;
//    private final ObjectTypeFieldMapper objectTypeFieldMapper;
//    private final ObjectTypeToArchetypeMapper objectTypeToArchetypeMapper;
//
//    private final RefValueRepository refValueRepo;
//    private final RepositoryResolver repositoryResolver;
//    private final EnumValueRepository enumValueRepository;
//    private final UserProfileJdbcRepository userProfileJdbcRepository;
//
//    private final CaseRepository caseRepository;
//    private final CaseMapper caseMapper;
//
//    public Map<String, Object> createObject(String objectType, List<Map<String, Object>> fields, String authorizationHeader) {
//
//        Map<String, Object> objectData = fields.stream()
//                .filter(e -> e.containsKey("name") && e.containsKey("value"))
//                .collect(Collectors.toMap(
//                        e -> e.get("name").toString(),
//                        e -> e.get("value")
//                ));
//
//        String oid = midPointClient
//                .createObject(objectType, objectData, authorizationHeader)
//                .block();
//
//        log.info("Объект типа '{}' создан, oid = {}", objectType, oid);
//        return Map.of("oid", oid);
//    }
//
//    public Mono<String> updateObject(String objectType, UUID oid, List<AttributeDelta> deltas, String authorizationHeader) {
//
//        List<Map<String, Object>> itemDeltaList = deltas.stream()
//                .map(delta -> {
//                    Map<String, Object> map = new HashMap<>();
//                    map.put("modificationType", delta.getModification());
//                    map.put("path", delta.getName().replace("_", "/"));
//
//                    Object value = delta.getValue();
//
//                    if (value instanceof String) {
//                        map.put("value", value);
//                    } else if (value instanceof List<?> list && list.stream().allMatch(i -> i instanceof Map)) {
//                        map.put("value", list);
//                    } else if (value instanceof List<?> list) {
//                        map.put("value", list);
//                    } else if (value instanceof Map<?, ?> mapValue) {
//                        map.put("value", List.of(mapValue));
//                    } else {
//                        map.put("value", List.of(value));
//                    }
//
//                    return map;
//                }).toList();
//
//        Map<String, Object> body = Map.of("objectModification", Map.of("itemDelta", itemDeltaList));
//
//        return midPointClient.updateObject(objectType, oid, body, authorizationHeader);
//    }
//
//    public void deleteObject(String objectType, UUID oid, String authorizationHeader) {
//        midPointClient.deleteObject(objectType, oid, authorizationHeader)
//                .doOnSuccess(unused -> log.info("Объект типа '{}' с oid='{}' успешно удалён", objectType, oid))
//                .doOnError(error -> log.error("Ошибка при удалении объекта типа '{}' с oid='{}': {}", objectType, oid, error.getMessage()))
//                .block();
//    }
//
//    public Map<String, Object> getFieldsByArchetypeOrByObjectType(String archetype, Pageable pageable) {
//        List<ObjectArchetypeFieldDto> fields = getFieldsOrFallback(archetype, pageable);
//        return buildResultMap(null, null, fields);
//    }
//
//    public Map<String, Object> searchFieldsByArchetype(String archetype, FilterRequest request, Pageable pageable) {
//        List<ObjectArchetypeFieldDto> fields = getFilteredFieldsOrFallback(archetype, request, pageable);
//        return buildResultMap(null, null, fields);
//    }
//
//    public Page<?> getObjectsByObjectType(String objectType, Pageable pageable) {
//        List<ObjectTypeFieldDto> fields = getNonExtObjectTypeFieldDtosByObjectType(objectType, null, null);
//
////        String tableName = fields.get(0).getTablename();
//        String tableName = "m_user";
//
//        Specification<?> spec = hasEmptyCondition();
//
//        FilterNode emptyFilter = new FilterNode();
//
//        return fetchAllDataFromTable(tableName, spec, pageable, List.of(emptyFilter));
//    }
//
//    public Page<?> searchObjectsByObjectType(String objectType, FilterRequest request, Pageable pageable) {
//        List<ObjectTypeFieldDto> fields = getNonExtObjectTypeFieldDtosByObjectType(objectType, null, request);
//
//        if (fields.isEmpty()) throw new IllegalArgumentException("Не указана таблица из которой нужно брать данные");
//
////        String tableName = fields.get(0).getTableName();
//        String tableName = "m_user";
//
//
//        Optional<Class<Object>> optionalClass = resolveEntityClass(tableName);
//        if (optionalClass.isEmpty()) {
//            throw new IllegalArgumentException("Не удалось определить entityClass для таблицы: " + tableName);
//        }
//        Class<Object> entityClass = optionalClass.get();
//
//        Specification<?> spec = buildSpec(entityClass, request);
//
//        return fetchAllDataFromTable(tableName, spec, pageable, request.getFilters());
//    }
//
//
//    public Map<String, Object> getFieldsByArchetypeAndOid(String archetype, UUID oid, FilterRequest request, Pageable pageable) {
//        log.info("[getFieldsByArchetypeAndOid] Входные параметры: archetype = {}, oid = {}, pageable = {}, request = {}", archetype, oid, pageable, request);
//
//        List<ObjectArchetypeFieldDto> fields = getFilteredFieldsOrFallback(archetype, request, pageable);
//        log.info("[getFieldsByArchetypeAndOid] Получено {} полей после фильтрации по архетипу", fields.size());
//
//        Map<String, List<ObjectArchetypeFieldDto>> fieldsByTable = getGroupedTables(fields);
//        log.info("[getFieldsByArchetypeAndOid] Сгруппировано по {} таблицам", fieldsByTable.size());
//
//        Map<String, Object> result = initializeResultMap(null, null, List.of());
//        List<Map<String, Object>> columnResults = new ArrayList<>();
//
//        for (Map.Entry<String, List<ObjectArchetypeFieldDto>> entry : fieldsByTable.entrySet()) {
//            String tableName = entry.getKey();
//            List<ObjectArchetypeFieldDto> tableFields = entry.getValue();
//
//            log.info("[getFieldsByArchetypeAndOid] Обработка таблицы: {}", tableName);
//            log.info("[getFieldsByArchetypeAndOid] Поля таблицы ({}): {}", tableFields.size(), tableFields.stream().map(ObjectArchetypeFieldDto::getFieldName).toList());
//
//            Optional<Class<Object>> entityClass = resolveEntityClass(tableName);
//            if (entityClass.isEmpty()) {
//                log.warn("[getFieldsByArchetypeAndOid] Не удалось определить entityClass для таблицы {}", tableName);
//                continue;
//            }
//
//            log.info("[getFieldsByArchetypeAndOid] Класс entity для таблицы {}: {}", tableName, entityClass.get().getSimpleName());
//
//            List<Object> entities = fetchTableData(oid, request, entityClass, tableName);
//            if (entities == null || entities.isEmpty()) {
//                log.warn("[getFieldsByArchetypeAndOid] Данные из таблицы {} не найдены для oid={}", tableName, oid);
//                continue;
//            }
//
//            log.info("[getFieldsByArchetypeAndOid] Получено {} записей из таблицы {}", entities.size(), tableName);
//
//            Map<String, Object> partial = buildResultMapFromEntity(entities.get(0), tableFields, request);
//            log.info("[getFieldsByArchetypeAndOid] Построен partial-результат: {}", partial.keySet());
//
//            mergeIdentifiers(result, partial);
//            columnResults.addAll(getColumns(partial));
//        }
//
//        result.put("columns", columnResults);
//        log.info("[getFieldsByArchetypeAndOid] Финальный результат содержит {} колонок", columnResults.size());
//
//        return result;
//    }
//
//
//    private List<ObjectArchetypeFieldDto> getFieldsOrFallback(String archetype, Pageable pageable) {
//        List<ObjectArchetypeFieldEntity> entities = objectArchetypeFieldRepository.findAll(
//                hasExtArchetype(archetype), pageable
//        ).getContent();
//
//        if (!entities.isEmpty()) {
//            return mapAndTrimArchetypeDtos(entities, null);
//        }
//
//        return getFallbackArchetypeDtos(archetype, pageable, null);
//    }
//
//    private List<ObjectArchetypeFieldDto> getFilteredFieldsOrFallback(String archetype, FilterRequest request, Pageable pageable) {
//        Specification<ObjectArchetypeFieldEntity> spec = hasExtArchetype(archetype)
//                .and(buildSpec(ObjectArchetypeFieldEntity.class, request));
//
//        List<ObjectArchetypeFieldEntity> entities = objectArchetypeFieldRepository.findAll(spec, pageable).getContent();
//
//        if (!entities.isEmpty()) {
//            return mapAndTrimArchetypeDtos(entities, request);
//        }
//
//        return getFallbackArchetypeDtos(archetype, pageable, request);
//    }
//
//    private List<ObjectArchetypeFieldDto> getFallbackArchetypeDtos(String archetype, Pageable pageable, FilterRequest request) {
//        List<ObjectTypeFieldDto> objectArchetypeFieldDtos = getObjectTypeFieldDtosByObjectType(archetype, pageable, request);
//
//        return objectArchetypeFieldDtos.stream()
//                .map(objectTypeToArchetypeMapper::toArchetypeDto)
//                .map(dto -> trimDto(dto, request))
//                .toList();
//    }
//
//    private List<Object> fetchDataFromTableByOid(Class<Object> entityClass, UUID oid, List<FilterNode> filters) {
//        return repositoryResolver.executeQueryForViews(entityClass, hasOid(oid), filters);
//    }
//
//    private List<ObjectTypeFieldDto> getObjectTypeFieldDtosByObjectType(String objectType, Pageable pageable, FilterRequest request) {
//        Specification<ObjectTypeFieldEntity> spec = hasObjectType(objectType)
//                .and(buildSpec(ObjectTypeFieldEntity.class, request));
//
//        List<ObjectTypeFieldDto> objectTypeFieldDtos = objectTypeFieldRepository.findAll(spec, pageable).stream()
//                .map(objectTypeFieldMapper::toDto)
//                .toList();
//        return objectTypeFieldDtos;
//    }
//
//    private List<ObjectTypeFieldDto> getNonExtObjectTypeFieldDtosByObjectType(String objectType, Pageable pageable, FilterRequest request) {
//        Specification<ObjectTypeFieldEntity> spec = hasObjectTypeAndNotExtTable(objectType);
//
//        if (pageable == null) {
//            pageable =  Pageable.unpaged();
//        }
//
//        return objectTypeFieldRepository.findAll(spec, pageable).stream()
//                .map(objectTypeFieldMapper::toDto)
//                .toList();
//    }
//
//
//    private List<ObjectArchetypeFieldDto> mapAndTrimArchetypeDtos(List<ObjectArchetypeFieldEntity> entities, FilterRequest request) {
//        return entities.stream()
//                .map(objectArchetypeFieldMapper::toDto)
//                .map(dto -> trimDto(dto, request))
//                .toList();
//    }
//
//    private ObjectArchetypeFieldDto trimDto(ObjectArchetypeFieldDto dto, FilterRequest request) {
//        return DtoFieldTrimmer.trim(dto,
//                request != null ? request.getFields() : null,
//                request != null ? request.getExcludeFields() : null);
//    }
//
//    private <T> Specification<T> buildSpec(Class<T> clazz, FilterRequest request) {
//        return FilterSpecificationBuilder.build(
//                null,
//                request != null ? request.getFilters() : null,
//                clazz,
//                null
//        );
//    }
//
//    private Specification<ObjectTypeFieldEntity> hasObjectType(String objectType) {
//        return (root, query, cb) ->
//                cb.and(cb.equal(root.get("objectType"), objectType), cb.equal(root.get("send"), true));
//    }
//
//    private static Specification<Object> hasOid(UUID oid) {
//        return (root, query, cb) -> cb.equal(root.get("oid"), oid);
//    }
//
//    private Specification<ObjectTypeFieldEntity> hasObjectTypeAndNotExtTable(String objectType) {
//        return (root, query, cb) -> cb.and(
//                cb.equal(cb.lower(root.get("objectType")), objectType.toLowerCase()),
//                cb.or(
//                        cb.isNull(root.get("extObject")),
//                        cb.equal(cb.trim(cb.lower(root.get("extObject"))), ""),
//                        cb.notLike(cb.lower(root.get("extObject")), "%ext%")
//                )
//        );
//    }
//
//    private Specification<Object> hasEmptyCondition() {
//        return (root, query, cb) -> cb.conjunction();
//    }
//
//    private Specification<ObjectArchetypeFieldEntity> hasExtArchetype(String extArchetype) {
//        return (root, query, cb) ->
//                cb.and(cb.equal(root.get("extArchetype"), extArchetype), cb.equal(root.get("send"), true));
//    }
//
//    private static Specification<Object> hasOidExtAttrName(UUID oid, String extAttrName) {
//        return (root, query, cb) ->
//                cb.and(
//                        cb.equal(root.get("oid"), oid),
//                        cb.equal(root.get("extAttrName"), extAttrName)
//                );
//    }
//
//    private Map<String, Object> buildResultMap(String oid, String vDisplayName, List<?> fields) {
//        Map<String, Object> resultMap = new LinkedHashMap<>();
//        resultMap.put("oid", String.valueOf(oid));
//        resultMap.put("vDisplayName", String.valueOf(vDisplayName));
//        resultMap.put("columns", fields);
//        return resultMap;
//    }
//
//    private Map<String, Object> initializeResultMap(String oid, String vDisplayName, List<?> fields) {
//        return buildResultMap(oid, vDisplayName, fields);
//    }
//
//    private Optional<Class<Object>> resolveEntityClass(String tableName) {
//        try {
//            return Optional.of((Class<Object>) repositoryResolver.resolveEntityClass(tableName));
//        } catch (Exception e) {
//            log.warn("⛔ Не удалось найти entityClass для tableName='{}': {}", tableName, e.getMessage());
//            return Optional.empty();
//        }
//    }
//
//    private List<Map<String, Object>> getColumns(Map<String, Object> tableResult) {
//        return (List<Map<String, Object>>) tableResult.getOrDefault("columns", List.of());
//    }
//    private void mergeIdentifiers(Map<String, Object> result, Map<String, Object> partial) {
//        if (isNullOrStringNull(result.get("oid")) && partial.containsKey("oid")) {
//            result.put("oid", partial.get("oid"));
//        }
//        if (isNullOrStringNull(result.get("vDisplayName")) && partial.containsKey("vDisplayName")) {
//            result.put("vDisplayName", partial.get("vDisplayName"));
//        }
//    }
//
//    private boolean isNullOrStringNull(Object value) {
//        return value == null || "null".equals(value);
//    }
//
//    private List<Object> fetchTableData(UUID oid, FilterRequest request, Optional<Class<Object>> entityClass, String tableName) {
//
//        List<FilterNode> combinedFilter = new ArrayList<>();
//
//        combinedFilter.add(new FilterNode("oid", FilterOperation.EQUAL, oid));
//
//        if(isExtTable(tableName)){
//            return fetchDataFromTableByOid(entityClass.get(), oid, combinedFilter);
//        }
//        return getMainTableData(oid, request, entityClass, combinedFilter);
//    }
//
//    private Page<?> fetchAllDataFromTable(String tableName, Specification<?> spec, Pageable pageable, List<FilterNode> filters) {
//        return repositoryResolver.executeQueryPaged(tableName, spec, pageable, filters);
//    }
//
//    private List<Object> getMainTableData(UUID oid, FilterRequest request, Optional<Class<Object>> entityClass, List<FilterNode> filters) {
//        Specification<Object> spec = buildSpec(entityClass.get(), request);
//        return repositoryResolver.executeQueryForViews(entityClass.get(), spec, filters);
//    }
//
//    private static Map<String, List<ObjectArchetypeFieldDto>> getGroupedTables(List<ObjectArchetypeFieldDto> fields) {
//        Map<String, List<ObjectArchetypeFieldDto>> fieldsByTable =
//                fields.stream()
//                        .filter(f -> f.getTableName() != null)
//                        .collect(Collectors.groupingBy(ObjectArchetypeFieldDto::getTableName));
//        return fieldsByTable;
//    }
//
//
//
//    private Map<String, Object> buildResultMapFromEntity(Object entity, List<ObjectArchetypeFieldDto> fields, FilterRequest request) {
//        Map<String, Object> row = new LinkedHashMap<>();
//
//        try {
//            UUID oid = (UUID) entity.getClass().getMethod("getOid").invoke(entity);
//            row.put("oid", oid);
//
//            Object vDisplayName = null;
//            try {
//                Method method = entity.getClass().getMethod("getVDisplayName");
//                vDisplayName = method.invoke(entity);
//            } catch (NoSuchMethodException ignored) {
//                // will fallback to reference field logic
//            }
//
//            if (vDisplayName == null) {
//                for (ObjectArchetypeFieldDto field : fields) {
//                    String tableField = field.getTableField();
//                    if (tableField == null) continue;
//
//                    Object value = null;
//                    try {
//                        if (isExtTable(field.getTableName())) {
//                            value = getExtValueByOidAndField(field.getTableName(), oid, tableField, request.getFilters());
//                        } else {
//                            String getterName = "get" + capitalize(tableField);
//                            value = entity.getClass().getMethod(getterName).invoke(entity);
//                        }
//                    } catch (Exception ignored) {}
//
//                    if (value != null && isReference(field.getExtType())) {
//                        vDisplayName = refValueRepo.getDisplayName(value.toString(), field.getExtObject());
//                        break;
//                    }
//                }
//            }
//
//            row.put("vDisplayName", vDisplayName);
//
//            List<ObjectArchetypeFieldDto> columns = new ArrayList<>();
//            for (ObjectArchetypeFieldDto field : fields) {
//                System.out.println("tableFields " + field.getTableField());
//                String tableField = field.getTableField();
//                if (tableField == null) continue;
//
//                Object value = null;
//                List<String> variables = null;
//                try {
//                    if (isExtTable(field.getTableName())) {
//                        value = getExtValueByOidAndField(field.getTableName(), oid, tableField, request.getFilters());
//                        if(field.getExtType().equals("Lookup"))
//                            variables = getEnumVariablesForExtType(field.getExtObject());
//
//                    } else {
//                        String getterName = "get" + capitalize(tableField);
//                        value = entity.getClass().getMethod(getterName).invoke(entity);
//                    }
//
//                } catch (Exception e) {
//                    log.info("⚠️ Не удалось извлечь значение поля '{}' из '{}': {}", tableField, field.getTableName(), e.getMessage());
//                }
//                field.setVariables(variables);
//                if (value instanceof byte[] bytes) {
//                    field.setValue(Base64.getEncoder().encodeToString(bytes));
//                } else {
//                    field.setValue(value != null ? value.toString() : null);
//                }
//                columns.add(field);
//            }
//
//            List<ObjectArchetypeFieldDto> trimmedColumns = columns.stream()
//                    .map(c -> trimDto(c, request)).toList();
//
//            row.put("columns", trimmedColumns);
//            return row;
//
//        } catch (Exception ex) {
//            throw new RuntimeException("Не удалось извлечь данные из entity: " + entity.getClass().getSimpleName(), ex);
//        }
//    }
//
//    private List<String> getEnumVariablesForExtType(String extObject) {
//        return enumValueRepository.findByEnumType(extObject)
//                .stream().
//                map(EnumValueEntity::getEnumValue)
//                .collect(Collectors.toList());
//    }
//
//    private boolean isExtTable(String tableName) {
//        return tableName != null && tableName.toLowerCase().contains("ext");
//    }
//
//    private String capitalize(String str) {
//        if (str == null || str.isEmpty()) return str;
//        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
//    }
//
//    private String getExtValueByOidAndField(String tableName, UUID oid, String extAttrName, List<FilterNode> filters) {
//        Class<?> entityClass = repositoryResolver.resolveEntityClass(tableName);
//
//        Specification<?> spec = hasOidExtAttrName(oid, extAttrName);
//
//        List<?> results = repositoryResolver.executeQueryForViews((Class<Object>) entityClass, (Specification<Object>) spec, filters);
//
//        if (results.isEmpty()) return null;
//
//        Object entity = results.get(0);
//        try {
//            Method getter = entity.getClass().getMethod("getExtAttrValue");
//            Object value = getter.invoke(entity);
//            return value != null ? value.toString() : null;
//        } catch (Exception e) {
//            log.warn("⚠️ Ошибка получения extAttrValue из {}: {}", entity.getClass().getSimpleName(), e.getMessage());
//            return null;
//        }
//    }
//
//
//
//    private boolean isReference(String extType) {
//        return ("ref".equalsIgnoreCase(extType) || "link".equalsIgnoreCase(extType));
//    }
//
//    public Page<Map<String, Object>> getByFieldNameAndArchetypeWithExtQuery(String archetype, String fieldName, FilterRequest request, Pageable pageable) {
//        Optional<ObjectArchetypeFieldEntity> optionalField =
//                objectArchetypeFieldRepository.findByExtArchetypeAndFieldName(archetype, fieldName);
//
//        if (optionalField.isEmpty()) {
//            optionalField = objectArchetypeFieldRepository.findByObjectTypeAndFieldName(archetype, fieldName);
//        }
//
//        ObjectArchetypeFieldEntity field = optionalField
//                .orElseThrow(() -> new IllegalArgumentException("Field not found"));
//
//        String tableName = field.getExtObject();
//        String whereClause = field.getExtWhereclause();
//
//        if (tableName == null || tableName.isBlank()) {
//            throw new IllegalArgumentException("ext_object (table/view) не указано");
//        }
//
//        Class<?> entityClass = repositoryResolver.resolveEntityClass(tableName);
//
//        Specification<?> whereSpec = repositoryResolver.buildWhereClauseSpec(whereClause, entityClass);
//        Specification<?> filterSpec = FilterSpecificationBuilder.build(
//                null,
//                request != null ? request.getFilters() : null,
//                entityClass,
//                null
//        );
//
//        List<FilterNode> whereNode = repositoryResolver.buildWhereClause(whereClause);
//
//        whereNode.addAll(request.getFilters());
//
//        @SuppressWarnings("unchecked")
//        Specification<Object> combinedSpec = ((Specification<Object>) whereSpec)
//                .and((Specification<Object>) filterSpec);
//
//        Page<Object> page = repositoryResolver.executeQueryPaged(
//                (Class<Object>) entityClass, combinedSpec, pageable, whereNode
//        );
//
//        List<Map<String, Object>> resultList = page.getContent().stream()
//                .map(this::toFieldMap)
//                .map(row -> DtoFieldTrimmer.trimMap(
//                        row,
//                        request != null ? request.getFields() : null,
//                        request != null ? request.getExcludeFields() : null
//                ))
//                .toList();
//
//        return new PageImpl<>(resultList, pageable, page.getTotalElements());
//    }
//
//    private Map<String, Object> toFieldMap(Object entity) {
//        Map<String, Object> map = new LinkedHashMap<>();
//        try {
//            for (Field field : entity.getClass().getDeclaredFields()) {
//                field.setAccessible(true);
//                map.put(field.getName(), field.get(entity));
//            }
//        } catch (Exception e) {
//            log.warn("Ошибка при маппинге entity: {}", e.getMessage(), e);
//        }
//        return map;
//    }
//
//
//    public Map<String, Object> getFieldsByObjectTypeAndArchetypeAndOid(
//            String objectType,
//            String archetype,
//            UUID oid,
//            FilterRequest request,
//            Pageable pageable
//    ) {
//        log.info("📥 Получение полей: objectType='{}', archetype='{}', oid='{}'", objectType, archetype, oid);
//
//        // Шаг 1: пробуем получить поля по archetype (extArchetype)
//        List<ObjectArchetypeFieldEntity> entities = objectArchetypeFieldRepository
//                .findAll(hasExtArchetype(archetype).and(buildSpec(ObjectArchetypeFieldEntity.class, request)), pageable)
//                .getContent();
//
//        List<ObjectArchetypeFieldDto> fieldDtos;
//
//        if (!entities.isEmpty()) {
//            log.info("✅ Нашли {} полей по archetype='{}'", entities.size(), archetype);
//            fieldDtos = mapAndTrimArchetypeDtos(entities, request);
//        } else {
//            log.warn("⚠️ Не найдено полей по archetype='{}'. Выполняем fallback по objectType='{}'", archetype, objectType);
//            List<ObjectTypeFieldDto> fallbackFields = objectTypeFieldRepository.findAll(
//                    hasObjectType(objectType.toUpperCase()).and(buildSpec(ObjectTypeFieldEntity.class, request)), pageable
//            ).stream().map(objectTypeFieldMapper::toDto).toList();
//
//            fieldDtos = fallbackFields.stream()
//                    .map(objectTypeToArchetypeMapper::toArchetypeDto)
//                    .map(dto -> trimDto(dto, request))
//                    .toList();
//        }
//
//        // 🛑 Если вообще нет полей — возвращаем пустой результат
//        if (fieldDtos.isEmpty()) {
//            log.warn("❌ Ни одного поля не найдено ни по archetype, ни по objectType. Возвращаем пустой ответ.");
//            return buildResultMap(null, null, List.of());
//        }
//
//        // Шаг 2: получаем значения полей по objectType и oid
//        Map<String, Object> fieldValues = getFieldValues(fieldDtos.get(0).getTableName(), oid, fieldDtos, request.getFilters());
//
//        log.info("📦 Проставляем значения полей для {} полей", fieldDtos.size());
//
//        log.info(fieldDtos.toString());
//
//        fieldDtos.forEach(dto -> {
//            String fieldName = dto.getTableField();
//            Object raw = fieldValues.get(fieldName);
//            String valueStr = raw != null ? raw.toString() : null;
//
//            dto.setValue(valueStr);
//            log.debug("🔄 Установлено значение для поля '{}': {}", fieldName, valueStr);
//        });
//
//        Object vDisplayName = fieldValues.get("vdisplayname");
//        String displayStr = vDisplayName != null ? vDisplayName.toString() : null;
//
//        log.info("vDisplayName: {}", displayStr);
//
//        return buildResultMap(
//                oid.toString(),
//                displayStr,
//                fieldDtos
//        );
//    }
//
//
//    private Map<String, Object> getFieldValues(String objectType, UUID oid, List<ObjectArchetypeFieldDto> fields, List<FilterNode> filters) {
//        log.info("Извлекаем значения полей из таблицы '{}', oid='{}'", objectType, oid);
//
//        Class<?> entityClass = repositoryResolver.resolveEntityClass(objectType);
//        log.info("Определён entityClass: {}", entityClass.getSimpleName());
//
//        Specification<?> spec = repositoryResolver.buildWhereClauseSpec("oid='" + oid + "'", entityClass);
//        List<FilterNode> combinedFilter = new ArrayList<>(filters != null ? filters : List.of());
//        combinedFilter.add(new FilterNode("oid", FilterOperation.EQUAL, oid));
//        List<?> results = repositoryResolver.executeUntypedQuery(entityClass, spec, combinedFilter);
//
//        if (results.isEmpty()) {
//            log.warn("⚠️ Не найден объект с oid='{}' в таблице '{}'", oid, objectType);
//            return Map.of();
//        }
//
//        Object entity = results.get(0);
//        Map<String, Object> values = new HashMap<>();
//
//        for (ObjectArchetypeFieldDto dto : fields) {
//            String fieldName = dto.getTableField();
//            try {
//                Field field = entityClass.getDeclaredField(fieldName);
//                field.setAccessible(true);
//                Object value = field.get(entity);
//                values.put(fieldName, value);
//
////                log.info("Поле '{}' успешно извлечено: {}", fieldName, value);
//            } catch (NoSuchFieldException nf) {
//                log.warn("Поле '{}' не найдено в классе '{}'", fieldName, entityClass.getSimpleName());
//                values.put(fieldName, null);
//            } catch (Exception e) {
//                log.error("Ошибка при извлечении поля '{}': {}", fieldName, e.getMessage(), e);
//                values.put(fieldName, null);
//            }
//        }
//
//        log.info("📤 Извлечены значения {} полей из '{}'", values.size(), objectType);
//        return values;
//    }
//
//
//}
