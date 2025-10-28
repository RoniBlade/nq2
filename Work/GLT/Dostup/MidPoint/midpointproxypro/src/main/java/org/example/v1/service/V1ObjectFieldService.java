package org.example.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.dto.ObjectTypeFieldDto;
import org.example.v1.entity.EnumValueEntity;
import org.example.v1.entity.ObjectTypeFieldEntity;
import org.example.v1.repository.EnumValueRepository;
import org.example.v1.repository.ObjectTypeFieldRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.stream.Collectors;

import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

@Slf4j
@Service
@RequiredArgsConstructor
public class V1ObjectFieldService {

    /* ====================== Constants ====================== */

    private static final int MAX_LOG_LEN = 500;

    private static final String EXT_LOOKUP = "lookup";
    private static final String EXT_REF    = "ref";
    private static final String EXT_LINK   = "link";

    /* ====================== Deps ====================== */

    private final StructureService structureService;
    private final ObjectTypeFieldRepository fieldsRepo;
    private final EnumValueRepository enumRepo;
    private final ObjectMapper objectMapper; // для JSON-парсинга

    @Value("${app.timing.object-fields.warn-ms:0}")
    private long warnThresholdMs;

    /* ====================== META ====================== */

    public Map<String, Object> metaByObjectType(String objectType, FilterRequest request) {
        List<String> columns     = safe(request != null ? request.getFields() : null);
        List<String> excludeCols = safe(request != null ? request.getExcludeFields() : null);

        StopWatch sw = new StopWatch("object-fields:meta");

        sw.start("load-fields");
        List<ObjectTypeFieldEntity> meta = selectFields(objectType, null, false, null, columns, excludeCols);
        sw.stop();

        sw.start("build-response");
        Map<String, Object> out = makeResponse(null, null, meta, false, Map.of());
        sw.stop();

        logTimings(sw, "META by objectType",
                ctx("objectType", objectType, "columns", columns, "exclude", excludeCols, "fieldsCount", meta.size()));
        return out;
    }

    public Map<String, Object> metaByObjectTypeOrArchetype(String objectType, String archetype, FilterRequest request) {
        List<String> columns     = safe(request != null ? request.getFields() : null);
        List<String> excludeCols = safe(request != null ? request.getExcludeFields() : null);

        StopWatch sw = new StopWatch("object-fields:meta");

        sw.start("load-fields(archetype?)");
        List<ObjectTypeFieldEntity> meta = selectUsingFallback(objectType, archetype, true, null, columns, excludeCols);
        sw.stop();

        sw.start("build-response");
        Map<String, Object> out = makeResponse(null, null, meta, false, Map.of());
        sw.stop();

        logTimings(sw, "META by objectType/archetype",
                ctx("objectType", objectType, "archetype", archetype, "columns", columns, "exclude", excludeCols, "fieldsCount", meta.size()));
        return out;
    }

    /* ====================== VALUES (ONE) ====================== */
    public Map<String, Object> valuesByObjectTypeAndOid(String objectType, UUID oid, FilterRequest request) {
        return valuesByObjectTypeOrArchetypeAndOid(objectType, null, oid, request);
    }

    public Map<String, Object> valuesByObjectTypeOrArchetypeAndOid(String objectType,
                                                                   String archetype,
                                                                   UUID oid,
                                                                   FilterRequest request) {
        List<String> columns     = safe(request != null ? request.getFields() : null);
        List<String> excludeCols = safe(request != null ? request.getExcludeFields() : null);

        String effectiveObjectParam = (nonBlank(archetype) && hasMappedFieldsForArchetype(objectType, archetype))
                ? archetype
                : objectType;

        if (log.isDebugEnabled()) {
            log.debug("REQUEST: values(one) | structure={} | object={} | oid={} | columns={} | exclude={}",
                    objectType, effectiveObjectParam, oid, oneLine(columns), oneLine(excludeCols));
        }

        StopWatch sw = new StopWatch("object-fields:values(one)");

        // 1) один объект из БД — через getObject(...)
        sw.start("structure-db");
        Map<String, Object> raw = structureService.getObject(
                objectType, effectiveObjectParam,
                oid != null ? oid.toString() : null, effectiveObjectParam
        );
        sw.stop();

        if (log.isDebugEnabled()) {
            log.debug("RAW(one) structure={}, object={} -> {} keys",
                    objectType, effectiveObjectParam, (raw != null ? raw.size() : 0));
        }

        // 🔹 Преобразуем бинарные поля (photo, jpegPhoto и т.п.) в Base64
        if (raw != null) {
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String key = entry.getKey().toLowerCase();
                Object val = entry.getValue();

                // Обрабатываем поля, где может быть фото
                if (key.contains("photo") || key.contains("image")) {
                    try {
                        if (val instanceof byte[] bytes) {
                            // Прямое преобразование из byte[]
                            entry.setValue("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes));

                        } else if (val instanceof String s) {
                            // PostgreSQL bytea приходит как строка типа "\xFFD8FF..."
                            if (s.startsWith("\\x")) {
                                byte[] bytes = parseHexBinary(s.substring(2));
                                entry.setValue(Base64.getEncoder().encodeToString(bytes));
                            }
                            // Иногда MidPoint возвращает Base64 напрямую — не трогаем
                        }
                    } catch (Exception e) {
                        log.warn("Ошибка преобразования фото [{}]: {}", key, e.getMessage());
                    }
                }
            }
        }

        // 2) мета с фолбэком
        sw.start("load-fields");
        List<ObjectTypeFieldEntity> meta = selectUsingFallback(objectType, archetype, true, raw, columns, excludeCols);
        sw.stop();

        // 3) ответ
        sw.start("build-response");
        Map<String, Object> out = makeResponse(oid, extractDisplayName(raw), meta, true, raw);
        sw.stop();

        logTimings(sw, "VALUES by type/archetype+oid",
                ctx("objectType", objectType, "archetype", archetype,
                        "effectiveObjectParam", effectiveObjectParam, "oid", oid,
                        "columns", columns, "exclude", excludeCols, "fieldsCount", meta.size()));

        return out;
    }


    /* ====================== Выбор эффективного OBJECT ====================== */

    private boolean hasMappedFieldsForArchetype(String objectType, String archetype) {
        if (!nonBlank(archetype)) return false;
        Sort sort = Sort.by(Sort.Order.asc("extorder"), Sort.Order.asc("fieldname"));
        List<ObjectTypeFieldEntity> all = fieldsRepo.findByObjecttypeAndSend(objectType, true, sort);
        boolean exists = all.stream()
                .filter(e -> archetype.equalsIgnoreCase(nullToEmpty(e.getArchetype())))
                .anyMatch(e -> nonBlank(e.getTablefield()));
        if (log.isDebugEnabled()) {
            log.debug("CHOOSE effectiveObjectParam: type={} arch={} -> mappedExists={}", objectType, archetype, exists);
        }
        return exists;
    }

    private List<ObjectTypeFieldEntity> selectUsingFallback(String objectType,
                                                            String archetype,
                                                            boolean withValues,
                                                            Map<String, Object> data,
                                                            List<String> columns,
                                                            List<String> excludeCols) {
        List<ObjectTypeFieldEntity> first = selectFields(objectType, archetype, withValues, data, columns, excludeCols);
        if (!first.isEmpty()) return first;
        if (nonBlank(archetype)) {
            log.debug("META fallback: no mapped fields for (type={}, arch={}, send=true) -> using base (ext_archetype IS NULL/blank)",
                    objectType, archetype);
        }
        return selectFields(objectType, null, withValues, data, columns, excludeCols);
    }

    /* ====================== Выбор полей ====================== */

    private List<ObjectTypeFieldEntity> selectFields(String objectType,
                                                     String archetype,
                                                     boolean withValues,
                                                     Map<String, Object> data,
                                                     List<String> includeColumns,
                                                     List<String> excludeColumns) {
        Sort sort = Sort.by(Sort.Order.asc("extorder"), Sort.Order.asc("fieldname"));
        List<ObjectTypeFieldEntity> all = fieldsRepo.findByObjecttypeAndSend(objectType, true, sort);

        List<ObjectTypeFieldEntity> base;
        if (nonBlank(archetype)) {
            String arch = archetype.trim();
            base = all.stream()
                    .filter(e -> arch.equalsIgnoreCase(nullToEmpty(e.getArchetype())))
                    .collect(Collectors.toList());
        } else {
            base = all.stream()
                    .filter(e -> !nonBlank(e.getArchetype()))
                    .collect(Collectors.toList());
        }

        base = base.stream()
                .filter(e -> nonBlank(e.getTablefield()))
                .collect(Collectors.toList());

        Set<String> includeSet = toLowerSet(includeColumns);
        Set<String> excludeSet = toLowerSet(excludeColumns);

        if (!includeSet.isEmpty()) {
            base = base.stream()
                    .filter(e -> {
                        String fn = nullToEmpty(e.getFieldname()).toLowerCase(Locale.ROOT);
                        String tf = nullToEmpty(e.getTablefield()).toLowerCase(Locale.ROOT);
                        return includeSet.contains(fn) || includeSet.contains(tf);
                    })
                    .collect(Collectors.toList());
        }
        if (!excludeSet.isEmpty()) {
            base = base.stream()
                    .filter(e -> {
                        String fn = nullToEmpty(e.getFieldname()).toLowerCase(Locale.ROOT);
                        String tf = nullToEmpty(e.getTablefield()).toLowerCase(Locale.ROOT);
                        return !excludeSet.contains(fn) && !excludeSet.contains(tf);
                    })
                    .collect(Collectors.toList());
        }

        if (withValues && data != null && !data.isEmpty()) {
            base = base.stream()
                    .filter(e -> hasPath(data, getFieldPath(e)))
                    .collect(Collectors.toList());
        }

        if (log.isDebugEnabled()) {
            log.debug("FIELDS selected: type={} arch={} withValues={} all={} final={}",
                    objectType, archetype, withValues, all.size(), base.size());
        }
        return base;
    }

    private static Set<String> toLowerSet(List<String> list) {
        if (list == null || list.isEmpty()) return Collections.emptySet();
        return list.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<String> safe(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }

    /* ====================== Сборка ответа ====================== */

    private Map<String, Object> makeResponse(UUID oid,
                                             String displayname,
                                             List<ObjectTypeFieldEntity> fieldsMeta,
                                             boolean withValues,
                                             Map<String, Object> raw) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("oid", oid);
        out.put("vdisplayname", displayname);
        out.put("columns", toDtos(fieldsMeta, withValues, raw));
        return out;
    }

    private List<ObjectTypeFieldDto> toDtos(List<ObjectTypeFieldEntity> meta,
                                            boolean withValues,
                                            Map<String, Object> data) {
        List<ObjectTypeFieldDto> out = new ArrayList<>(meta.size());

        Map<String, List<String>> lookupCache = new HashMap<>();

        for (ObjectTypeFieldEntity e : meta) {
            ObjectTypeFieldDto dto = new ObjectTypeFieldDto();
            dto.setOid(e.getOid());
            dto.setFieldname(e.getFieldname());
            dto.setFieldtype(e.getFieldtype());
            dto.setObjecttype(e.getObjecttype());
            dto.setArchetype(e.getArchetype());
            dto.setTablefield(e.getTablefield());
            dto.setSend(e.getSend());
            dto.setVisible(e.getVisible());
            dto.setRead(e.getRead());
            dto.setTabname(e.getTabname());
            dto.setExtorder(e.getExtorder());
            dto.setExttype(e.getExttype());
            dto.setExtobject(e.getExtobject());
            dto.setExtwhereclause(e.getExtwhereclause());
            dto.setExtnotes(e.getExtnotes());

            final String extTypeLower = nullToEmpty(e.getExttype()).toLowerCase(Locale.ROOT);
            final String targetType   = nonBlank(e.getExtobject()) ? e.getExtobject().trim() : null;

            if (withValues) {
                String path   = getFieldPath(e);
                Object rawVal = extractByPath(data, path);

                // value — отдаем объект/массив/JsonNode как есть; строковый JSON парсим в JsonNode
                Object valueObj = toJsonValue(rawVal);

                // displayvalue — «оригинал» для *norm (если есть), иначе raw
                Object origOrRaw = tryUseOrigForNormRaw(e, data, rawVal);
                Object displayObj = toJsonValue(origOrRaw);

                dto.setValue(valueObj);
                dto.setDisplayvalue(displayObj);
            } else {
                dto.setValue(null);
                dto.setDisplayvalue(null);
            }

            List<String> values = Collections.emptyList();
            if (EXT_LOOKUP.equals(extTypeLower) && targetType != null) {
                values = lookupCache.computeIfAbsent(targetType, this::loadLookupOptionsSafe);
            }
            dto.setValues(values);

            out.add(dto);
        }
        return out;
    }

    /* ====================== Helpers: value/display/lookup ====================== */

    /**
     * Превращает вход в «правильный» JSON-объект для выдачи:
     *  - JsonNode/Map/Collection/array → как есть;
     *  - String вида '{...}' или '[...]' → парсит в JsonNode;
     *  - остальное → как есть (числа/строки и т. п.).
     */
    private Object toJsonValue(Object v) {
        if (v == null) return null;

        if (v instanceof JsonNode) return v;
        if (v instanceof Map<?, ?>) return v;
        if (v instanceof Collection<?>) return v;
        if (v.getClass().isArray()) return v;

        if (v instanceof CharSequence s) {
            String t = s.toString().trim();
            if (looksLikeJson(t)) {
                try {
                    return objectMapper.readTree(t);
                } catch (Exception e) {
                    // оставим как строку ниже
                }
            }
            return s.toString();
        }
        return v;
    }

    private static boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.length() < 2) return false;
        char f = t.charAt(0), l = t.charAt(t.length() - 1);
        return (f == '{' && l == '}') || (f == '[' && l == ']');
    }

    private Object tryUseOrigForNormRaw(ObjectTypeFieldEntity e, Map<String, Object> data, Object fallback) {
        String path = getFieldPath(e);
        if (path == null) return fallback;

        String candidateKey = getOriginalKey(path);
        if (candidateKey == null) return fallback;

        Object orig = extractByPath(data, candidateKey);
        return (orig != null) ? orig : fallback;
    }

    private String getFieldPath(ObjectTypeFieldEntity e) {
        if (nonBlank(e.getTablefield())) return e.getTablefield();
        return nonBlank(e.getFieldname()) ? e.getFieldname() : null;
    }

    private String getOriginalKey(String key) {
        if (!nonBlank(key)) return null;
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith("norm")) {
            return key.substring(0, key.length() - 4) + "orig";
        }
        if (!key.contains(".")) return key + "orig";
        return null;
    }

    private List<String> loadLookupOptionsSafe(String enumType) {
        try {
            List<EnumValueEntity> list = enumRepo.findByEnumtype(enumType);
            if (list == null || list.isEmpty()) return Collections.emptyList();
            return list.stream()
                    .map(EnumValueEntity::getEnumvalue)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            log.warn("Lookup options load failed for enumType='{}': {}", enumType, oneLine(ex.getMessage()));
            return Collections.emptyList();
        }
    }

    /* ====================== Path utils ====================== */

    private Object extractByPath(Map<String, Object> map, String path) {
        if (map == null || !nonBlank(path)) return null;
        if (map.containsKey(path)) return map.get(path);

        String lower = path.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(lower)) {
                return e.getValue();
            }
        }

        if (path.contains(".")) {
            String[] parts = path.split("\\.");
            Object cur = map;
            for (String p : parts) {
                if (!(cur instanceof Map<?, ?> m)) return null;
                Object next = m.containsKey(p) ? m.get(p) : findCaseInsensitive(m, p);
                if (next == null) return null;
                cur = next;
            }
            return cur;
        }
        return null;
    }

    private boolean hasPath(Map<String, Object> map, String path) {
        if (map == null || !nonBlank(path)) return false;
        if (map.containsKey(path)) return true;

        String lower = path.toLowerCase(Locale.ROOT);
        for (String k : map.keySet()) {
            if (k != null && k.toLowerCase(Locale.ROOT).equals(lower)) return true;
        }

        if (path.contains(".")) {
            String[] parts = path.split("\\.");
            Object cur = map;
            for (String p : parts) {
                if (!(cur instanceof Map<?, ?> m)) return false;
                if (m.containsKey(p)) { cur = m.get(p); continue; }
                String hit = null;
                for (Object key : m.keySet()) {
                    if (key instanceof String s && s.equalsIgnoreCase(p)) { hit = s; break; }
                }
                if (hit == null) return false;
                cur = m.get(hit);
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Object findCaseInsensitive(Map<?, ?> m, String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object k = e.getKey();
            if (k instanceof String s && s.toLowerCase(Locale.ROOT).equals(lower)) {
                return e.getValue();
            }
        }
        return null;
    }

    /* ====================== Display name helpers ====================== */

    private String extractDisplayName(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) return null;
        Object dn = firstNonNull(
                raw.get("displayname"),
                raw.get("vdisplayname"),
                raw.get("nameorig"),
                raw.get("fullnameorig"),
                raw.get("nicknameorig"),
                raw.get("titleorig")
        );
        return dn != null ? String.valueOf(dn) : null;
    }

    private static Object firstNonNull(Object... arr) {
        if (arr == null) return null;
        for (Object o : arr) if (o != null) return o;
        return null;
    }

    private static String objToString(Object v) {
        if (v == null) return null;
        if (v instanceof String s) return s;
        return String.valueOf(v);
    }

    /* ====================== Logging & utils ====================== */

    // перегрузка с контекстом (чтобы компилировались вызовы с ctx(...))
    private void logTimings(StopWatch sw, String label, Map<String, ?> ctx) {
        long totalMs = sw.getTotalTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("TIMINGS [").append(label).append("] total=").append(totalMs).append("ms");
        Arrays.stream(sw.getTaskInfo()).forEach(t ->
                sb.append(" | ").append(t.getTaskName()).append("=").append(t.getTimeMillis()).append("ms"));

        if (warnThresholdMs > 0 && totalMs >= warnThresholdMs) {
            log.warn("{} | ctx={}", sb, oneLine(ctx));
        } else if (log.isDebugEnabled()) {
            log.debug("{} | ctx={}", sb, oneLine(ctx));
            log.debug("\n{}", sw.prettyPrint());
        }
    }

    private void logTimings(StopWatch sw, String label) {
        long totalMs = sw.getTotalTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("TIMINGS [").append(label).append("] total=").append(totalMs).append("ms");
        Arrays.stream(sw.getTaskInfo()).forEach(t ->
                sb.append(" | ").append(t.getTaskName()).append("=").append(t.getTimeMillis()).append("ms"));

        if (warnThresholdMs > 0 && totalMs >= warnThresholdMs) {
            log.warn("{}", sb);
        } else if (log.isDebugEnabled()) {
            log.debug("{}", sb);
            log.debug("\n{}", sw.prettyPrint());
        }
    }

    private static Map<String, Object> ctx(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        if (kv == null) return m;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = String.valueOf(kv[i]);
            Object v = kv[i + 1];
            m.put(k, v);
        }
        return m;
    }

    private static String oneLine(Object v) {
        if (v == null) return "null";
        String s = String.valueOf(v).replaceAll("\\s+", " ").trim();
        return s.length() > MAX_LOG_LEN ? s.substring(0, MAX_LOG_LEN) + " …" : s;
    }

    private static boolean nonBlank(String s) { return s != null && !s.isBlank(); }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
