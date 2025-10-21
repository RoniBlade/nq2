package org.example.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.util.field.DtoFieldTrimmer;
import org.example.v1.repository.JdbcRepository;
import org.example.v1.util.SqlUtil;
import org.example.v1.util.V1RepositoryResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StructureService {

    private static final int MAX_LOG_LEN = 500;
    private static final String FIELD_TOTALCOUNT = "totalcount";

    private final V1RepositoryResolver repositoryResolver;
    private final JdbcRepository jdbcRepo;
    private final ObjectMapper objectMapper;

    /** Если > 0 и суммарное время запроса >= порога — логируем в WARN, иначе в DEBUG */
    @Value("${app.timing.structure.warn-ms:0}")
    private long warnThresholdMs;

    public boolean checkStructureExist(String structure) {
        return repositoryResolver.getFunctionName(structure) != null;
    }

    /* ===================== ОДИН ОБЪЕКТ (2 параметра функции) ===================== */

    /**
     * getObject (получить один объект): функция с двумя параметрами (пример:
     * public.d_get_user(useroid text, p_object text)).
     */
    public Map<String, Object> getObject(String structure,
                                         String object,
                                         String idOrOid,
                                         String pObject) {
        return getObject(structure, object, idOrOid, pObject, null, null);
    }


    public Map<String, Object> getObject(String idOrOid) {
        return getObject(null, null, idOrOid, null, null, null);
    }

    private Map<String, Object> getObject(String structure,
                                         String object,
                                         String idOrOid,
                                         String pObject,
                                         List<String> includeFields,
                                         List<String> excludeFields) {
        StopWatch sw = new StopWatch("structure:" + structure + ":one");

        // build
        sw.start("build");
        String functionName = repositoryResolver.getFunctionName(structure);
        sw.stop();

        if (log.isDebugEnabled()) {
            log.debug("REQUEST ONE: structure={}, object={}, idOrOid={}, pObject={}, includeFields={}, excludeFields={}",
                    structure, oneLine(object), oneLine(idOrOid), oneLine(pObject),
                    oneLine(includeFields), oneLine(excludeFields));
        }

        // DB (2 параметра функции)
        sw.start("db");
        Map<String, Object> row = jdbcRepo.getOneFromFunction(functionName, idOrOid, pObject);
        sw.stop();

        // Если функция вернула JSON в одной колонке — распакуем
        Map<String, Object> unpacked = tryUnpackJsonSingleColumn(row);

        if (log.isDebugEnabled()) {
            log.debug("RESULT ONE: structure={}, object={} | map={}",
                    structure, oneLine(object), oneLine(unpacked));
        }

        // trim
        sw.start("trim");
        Map<String, Object> trimmed = DtoFieldTrimmer.trimMap(new LinkedHashMap<>(unpacked), includeFields, excludeFields);
        sw.stop();

        // timings
        logTimings(sw, structure + ":one");
        return trimmed;
    }

    /** Если карта содержит ровно одну текстовую колонку с JSON — распаковать в Map; иначе вернуть как есть. */
    @SuppressWarnings("unchecked")
    Map<String, Object> tryUnpackJsonSingleColumn(Map<String, Object> row) {
        if (row == null || row.isEmpty()) return new LinkedHashMap<>();
        if (row.size() != 1) return new LinkedHashMap<>(row);

        Map.Entry<String, Object> entry = row.entrySet().iterator().next();
        Object val = entry.getValue();
        if (val == null) return new LinkedHashMap<>();

        String json = String.valueOf(val).trim(); // важно: PGobject и т.п.
        if (json.isEmpty()) return new LinkedHashMap<>();

        if (json.startsWith("{") || json.startsWith("[")) {
            try {
                Object parsed = objectMapper.readValue(json, Object.class);
                if (parsed instanceof Map<?, ?> m) {
                    return new LinkedHashMap<>((Map<String, Object>) m);
                } else {
                    LinkedHashMap<String, Object> wrap = new LinkedHashMap<>();
                    wrap.put("value", parsed);
                    return wrap;
                }
            } catch (Exception e) {
                log.warn("Cannot parse JSON returned by function (col='{}', type={}): {}",
                        entry.getKey(),
                        val.getClass().getName(),
                        oneLine(e.getMessage()));
                return new LinkedHashMap<>(row);
            }
        }
        return new LinkedHashMap<>(row);
    }

    /* ===================== ПУБЛИЧНЫЕ API для списков (5 параметров функции) ===================== */

    /** Старый API (совместимость): из FilterRequest */
    public Page<Map<String, Object>> getDataStructure(String structure,
                                                      String object,
                                                      FilterRequest request,
                                                      Pageable pageable) {
        List<FilterNode> filters = (request != null && request.getFilters() != null)
                ? request.getFilters() : Collections.emptyList();
        List<String> include = (request != null) ? request.getFields() : null;
        List<String> exclude = (request != null) ? request.getExcludeFields() : null;

        return fetchPage(structure, object, filters, null, include, exclude, pageable);
    }

    public Page<Map<String, Object>> getDataStructure(String functionName,
            FilterRequest request,
            Pageable pageable) {
        List<FilterNode> filters = (request != null && request.getFilters() != null)
                ? request.getFilters() : Collections.emptyList();
        List<String> include = (request != null) ? request.getFields() : null;
        List<String> exclude = (request != null) ? request.getExcludeFields() : null;

        return fetchPage(functionName, null, filters, null, include, exclude, pageable);
    }

    /** Новый API: сразу список фильтров (без raw-условия) */
    public Page<Map<String, Object>> getDataStructure(String structure,
                                                      String object,
                                                      List<FilterNode> filters,
                                                      Pageable pageable) {
        return fetchPage(structure, object,
                filters != null ? filters : Collections.emptyList(),
                null, null, null, pageable);
    }

    /** Новый API: список фильтров + сырое условие (AND) */
    public Page<Map<String, Object>> getDataStructure(String structure,
                                                      String object,
                                                      List<FilterNode> filters,
                                                      String rawAndCondition,
                                                      Pageable pageable) {
        return fetchPage(structure, object,
                filters != null ? filters : Collections.emptyList(),
                rawAndCondition, null, null, pageable);
    }

    /** Обёртка: только сырое условие */
    public Page<Map<String, Object>> getDataStructureWhere(String structure,
                                                           String object,
                                                           String whereOrCondition,
                                                           Pageable pageable) {
        return fetchPage(structure, object,
                Collections.emptyList(), whereOrCondition, null, null, pageable);
    }

    /* ===================== ВНУТРЕННЕЕ ЯДРО СПИСКОВ ===================== */

    private Page<Map<String, Object>> fetchPage(String structure,
                                                String object,
                                                List<FilterNode> filters,
                                                String rawAndCondition,
                                                List<String> includeFields,
                                                List<String> excludeFields,
                                                Pageable pageable) {

        StopWatch sw = new StopWatch("structure:" + structure);

        // ---------- build SQL ----------
        sw.start("build-sql");

        SqlUtil.Clause whereClause = SqlUtil.toWhere(filters != null ? filters : Collections.emptyList());
        String filterCond = whereClause.sql().isBlank()
                ? ""
                : SqlUtil.inlineWhereForFunction(whereClause);

        String extraCond = normalizeCondition(rawAndCondition);
        String where = mergeWhere(filterCond, extraCond);

        String orderBy = SqlUtil.toOrderBy(pageable);
        long limit  = SqlUtil.limit(pageable);
        long offset = SqlUtil.offset(pageable);

        String functionName = repositoryResolver.getFunctionName(structure);
        sw.stop();

        if (log.isDebugEnabled()) {
            log.debug("REQUEST LIST: structure={}, object={}, page={}, size={}, sort={}, filtersCount={}, rawAndCondition={}, includeFields={}, excludeFields={}",
                    structure,
                    oneLine(object),
                    pageable != null ? pageable.getPageNumber() : null,
                    pageable != null ? pageable.getPageSize() : null,
                    pageable != null ? oneLine(pageable.getSort()) : null,
                    (filters != null ? filters.size() : 0),
                    oneLine(rawAndCondition),
                    oneLine(includeFields),
                    oneLine(excludeFields)
            );
        }



        // ---------- DB call (5 параметров функции) ----------
        sw.start("db");
        List<Map<String, Object>> content = jdbcRepo.getDataFromFunction(
                functionName,
                where,
                orderBy,
                limit,
                offset,
                object
        );
        sw.stop();

        // ---------- totalcount ----------
        sw.start("totalcount");
        long total = 0L;
        if (!content.isEmpty()) {
            Object tc = content.get(0).get(FIELD_TOTALCOUNT);
            if (tc instanceof Number n) total = n.longValue();
            else if (tc != null) {
                try { total = Long.parseLong(tc.toString()); } catch (NumberFormatException ignored) {}
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("TOTALCOUNT [{}]: total={} | pageRows={} | where={} | orderBy={}",
                    structure, total, content.size(), oneLine(where), oneLine(orderBy));
        }
        sw.stop();

        // ---------- trim ----------
        sw.start("trim");
        List<Map<String, Object>> trimmed = content.stream()
                .map(row -> {
                    Map<String, Object> copy = new LinkedHashMap<>(row);
                    copy.remove(FIELD_TOTALCOUNT);
                    return DtoFieldTrimmer.trimMap(copy, includeFields, excludeFields);
                })
                .collect(Collectors.toList());
        sw.stop();

        // ---------- page ----------
        sw.start("page");
        Page<Map<String, Object>> page = new PageImpl<>(trimmed, pageable, total);
        sw.stop();

        logTimings(sw, structure);
        return page;
    }





    public Page<Map<String, Object>> fetch( // TODO сделать приватным
            String functionName,
            String additionalParam,
            String object,
            List<FilterNode> filters,
            String rawAndCondition,
            List<String> includeFields,
            List<String> excludeFields,
            Pageable pageable) {

        StopWatch sw = new StopWatch("structure:" + functionName);

        // ---------- build SQL ----------
        sw.start("build-sql");

        SqlUtil.Clause whereClause = SqlUtil.toWhere(filters != null ? filters : Collections.emptyList());
        String filterCond = whereClause.sql().isBlank()
                ? ""
                : SqlUtil.inlineWhereForFunction(whereClause);

        String extraCond = normalizeCondition(rawAndCondition);
        String where = mergeWhere(filterCond, extraCond);

        String orderBy = SqlUtil.toOrderBy(pageable);
        long limit  = SqlUtil.limit(pageable);
        long offset = SqlUtil.offset(pageable);

        sw.stop();

        // ---------- DB call (5 параметров функции) ----------
        sw.start("db");

        List<Map<String, Object>> content = List.of();

        content = jdbcRepo.getDataForAssociation(functionName, object, additionalParam, limit, offset, orderBy, where);

        sw.stop();

        // ---------- totalcount ----------
        sw.start("totalcount");
        long total = 0L;
        if (!content.isEmpty()) {
            Object tc = content.get(0).get(FIELD_TOTALCOUNT);
            if (tc instanceof Number n) total = n.longValue();
            else if (tc != null) {
                try { total = Long.parseLong(tc.toString()); } catch (NumberFormatException ignored) {}
            }
        }
        sw.stop();

        // ---------- trim ----------
        sw.start("trim");
        List<Map<String, Object>> trimmed = content.stream()
                .map(row -> {
                    Map<String, Object> copy = new LinkedHashMap<>(row);
                    copy.remove(FIELD_TOTALCOUNT);
                    return DtoFieldTrimmer.trimMap(copy, includeFields, excludeFields);
                })
                .collect(Collectors.toList());
        sw.stop();

        // ---------- page ----------
        sw.start("page");
        Page<Map<String, Object>> page = new PageImpl<>(trimmed, pageable, total);
        sw.stop();

        logTimings(sw, functionName);
        return page;
    }

    /* ===================== ВСПОМОГАТЕЛЬНЫЕ ===================== */

    private void logTimings(StopWatch sw, String label) {
        long totalMs = sw.getTotalTimeMillis();
        String sb = String.format("TIMINGS [%s] total=%dms", label, totalMs);
        if (warnThresholdMs > 0 && totalMs >= warnThresholdMs) {
            log.warn(sb);
        } else if (log.isDebugEnabled()) {
            log.debug(sb);
            log.debug("\n{}", sw.prettyPrint());
        }
    }

    private static String normalizeCondition(String cond) {
        if (cond == null) return "";
        String s = cond.trim();
        if (s.isEmpty()) return "";
        String up = s.toUpperCase(Locale.ROOT);
        if (up.startsWith("WHERE ")) s = s.substring(6).trim();
        return s;
    }

    private static String mergeWhere(String filterCond, String extraCond) {
        boolean hasFilter = filterCond != null && !filterCond.isBlank();
        boolean hasExtra  = extraCond  != null && !extraCond.isBlank();
        if (!hasFilter && !hasExtra) return "";
        if (hasFilter && !hasExtra)  return "WHERE " + filterCond;
        if (!hasFilter)              return "WHERE (" + extraCond + ")";
        return "WHERE (" + filterCond + ") AND (" + extraCond + ")";
    }

    private static String oneLine(Object v) {
        if (v == null) return "null";
        String s = v.toString().replaceAll("\\s+", " ").trim();
        return s.length() > MAX_LOG_LEN ? s.substring(0, MAX_LOG_LEN) + " …" : s;
    }

    @SuppressWarnings("unused")
    private static String sqlQuote(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        String s = v.toString().replace("'", "''").replaceAll("\\s+", " ").trim();
        if (s.length() > MAX_LOG_LEN) s = s.substring(0, MAX_LOG_LEN) + " …";
        return "'" + s + "'";
    }
}
