// RequestViewDaoImpl.java (окончательная версия)
package com.glt.idm.rest.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Repository
public class RequestViewDaoImpl implements RequestViewDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "state", "casedatecreate", "requestername", "requesterfullname",
            "requestertitle", "requesteremail", "requesterphone", "targetoid", "targetnamenorm"
    );

    public RequestViewDaoImpl(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public List<Map<String, Object>> getRequestsFiltered(int offset, int size, Map<String, Object> filter) {
        Map<String, Object> params = new HashMap<>();
        AtomicInteger paramIndex = new AtomicInteger(0);
        String whereClause = buildWhereClause(filter, params, paramIndex);

        String sql = "SELECT * FROM glt_my_request_v"
                + (whereClause.isEmpty() ? "" : " WHERE " + whereClause)
                + " LIMIT :size OFFSET :offset";

        params.put("size", size);
        params.put("offset", offset);

        return jdbc.query(sql, params, new ColumnMapRowMapper());
    }

    @Override
    public int getFilteredCount(Map<String, Object> filter) {
        Map<String, Object> params = new HashMap<>();
        AtomicInteger paramIndex = new AtomicInteger(0);
        String whereClause = buildWhereClause(filter, params, paramIndex);

        String countSql = "SELECT COUNT(*) FROM glt_my_request_v"
                + (whereClause.isEmpty() ? "" : " WHERE " + whereClause);

        return jdbc.queryForObject(countSql, params, Integer.class);
    }

    @Override
    public List<Map<String, Object>> getRequestsFromView(int offset, int size,
            Map<String, String> filters,
            String userOid) {
        StringBuilder sql = new StringBuilder("""
        SELECT
            caseoid, casedatecreate, objecttype,
            objectname::jsonb as objectname,
            objectdisplayname, objectdescription,
            requestername, requesterfullname,
            requestertitle, asrequesterorganization,
            requesteremail, requesterphone,
            closetime, state, targetoid, targetnamenorm
        FROM glt_my_request_v
    """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", offset)
                .addValue("size", size);

        boolean hasWhere = false;
        for (Map.Entry<String, String> e : filters.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (StringUtils.hasText(value) && ALLOWED_FIELDS.contains(key)) {
                if (!hasWhere) {
                    sql.append(" WHERE ");
                    hasWhere = true;
                } else {
                    sql.append(" AND ");
                }
                sql.append(key).append(" ILIKE :").append(key);
                params.addValue(key, "%" + value + "%");
            }
        }

        sql.append(" OFFSET :offset LIMIT :size");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String column = meta.getColumnLabel(i);
                Object value = rs.getObject(i);

                if ("objectname".equalsIgnoreCase(column)) {
                    try {
                        value = objectMapper.readValue(
                                rs.getString(i),
                                new TypeReference<Map<String, Object>>() {}
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing JSON", e);
                    }
                }

                row.put(column.toLowerCase(Locale.ROOT), value);
            }
            return row;
        });
    }

    private String buildWhereClause(Map<String, Object> filter, Map<String, Object> params, AtomicInteger paramIndex) {
        if (filter == null || filter.isEmpty()) return "";

        if (filter.containsKey("and")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subFilters = (List<Map<String, Object>>) filter.get("and");
            return subFilters.stream()
                    .map(sub -> "(" + buildWhereClause(sub, params, paramIndex) + ")")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(" AND "));
        }

        if (filter.containsKey("or")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subFilters = (List<Map<String, Object>>) filter.get("or");
            return subFilters.stream()
                    .map(sub -> "(" + buildWhereClause(sub, params, paramIndex) + ")")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(" OR "));
        }

        if (filter.containsKey("not")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subFilter = (Map<String, Object>) filter.get("not");
            String clause = buildWhereClause(subFilter, params, paramIndex);
            return clause.isEmpty() ? "" : "NOT (" + clause + ")";
        }

        String field = (String) filter.get("field");
        String operator = (String) filter.get("operator");
        Object value = filter.get("value");

        if (!ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Фильтрация по полю '" + field + "' не разрешена");
        }

        String paramName = "p" + paramIndex.incrementAndGet();
        String condition;

        switch (operator) {
            case "equal":
                condition = field + " = :" + paramName;
                params.put(paramName, value);
                break;
            case "contains":
                condition = field + " ILIKE :" + paramName;
                params.put(paramName, "%" + value + "%");
                break;
            case "greaterThan":
                condition = field + " > :" + paramName;
                params.put(paramName, value);
                break;
            case "greaterThanOrEqual":
                condition = field + " >= :" + paramName;
                params.put(paramName, value);
                break;
            case "lessThan":
                condition = field + " < :" + paramName;
                params.put(paramName, value);
                break;
            case "lessThanOrEqual":
                condition = field + " <= :" + paramName;
                params.put(paramName, value);
                break;
            default:
                throw new IllegalArgumentException("Неподдерживаемый оператор: " + operator);
        }

        return condition;
    }
}
