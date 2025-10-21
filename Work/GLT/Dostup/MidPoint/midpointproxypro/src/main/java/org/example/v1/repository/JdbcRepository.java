package org.example.v1.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.v1.util.JsonAwareColumnMapRowMapper;
import org.example.v1.util.SqlUtil;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Repository
public class JdbcRepository {

    protected final JdbcTemplate jdbc;

    /** Единый маппер: json/jsonb -> JsonNode (через PGobject), остальное по умолчанию */
    private static final ColumnMapRowMapper JSON_AWARE_MAPPER = new JsonAwareColumnMapRowMapper();

    /**
     * SELECT * FROM schema.fn(?, ?, ?, ?) — (p_limit, p_offset, p_where, p_order_by)
     */
    public List<Map<String, Object>> getDataFromFunction(
            String functionName,
            String whereClause,
            String orderBy,
            Long limit,
            Long offset
    ) {
        assertIdentifierSafe(functionName);

        String whereExpr = stripLeading(whereClause, "WHERE");
        String orderExpr = stripLeading(orderBy, "ORDER BY");

        Integer lim = (limit == null) ? null : Math.toIntExact(limit);
        Integer off = (offset == null) ? null : Math.toIntExact(offset);

        List<Object> args = new ArrayList<>(4);
        List<Integer> types = new ArrayList<>(4);

        args.add(lim);                           types.add(Types.INTEGER);
        args.add(off);                           types.add(Types.INTEGER);
        args.add(isBlank(whereExpr) ? null : whereExpr); types.add(Types.VARCHAR);
        args.add(isBlank(orderExpr) ? null : orderExpr); types.add(Types.VARCHAR);

        String sql = "SELECT * FROM " + functionName + "(?, ?, ?, ?)";
        return jdbc.query(sql, args.toArray(), toIntArray(types), JSON_AWARE_MAPPER);
    }

    /** Перегрузка: WHERE как SqlUtil.Clause — инлайн внутри репо */
    public List<Map<String, Object>> getDataFromFunction(
            String functionName,
            SqlUtil.Clause whereClause,
            String orderBy,
            Long limit,
            Long offset
    ) {
        String inlined = (whereClause == null) ? "" : SqlUtil.inlineWhereForFunction(whereClause);
        return getDataFromFunction(functionName, inlined, orderBy, limit, offset);
    }

    /**
     * SELECT * FROM schema.fn(?, ?, ?, ?, ?) — (p_limit, p_offset, p_where, p_order_by, p_object)
     */
    public List<Map<String, Object>> getDataFromFunction(
            String functionName,
            String where,
            String orderBy,
            Long limit,
            Long offset,
            String object
    ) {
        assertIdentifierSafe(functionName);

        String whereExpr = stripLeading(where, "WHERE");
        String orderExpr = stripLeading(orderBy, "ORDER BY");

        Integer lim = (limit == null) ? null : Math.toIntExact(limit);
        Integer off = (offset == null) ? null : Math.toIntExact(offset);

        List<Object> args  = new ArrayList<>(5);
        List<Integer> types = new ArrayList<>(5);

        args.add(lim);                                   types.add(Types.INTEGER);
        args.add(off);                                   types.add(Types.INTEGER);
        args.add(isBlank(whereExpr) ? null : whereExpr); types.add(Types.VARCHAR);
        args.add(isBlank(orderExpr) ? null : orderExpr); types.add(Types.VARCHAR);
        args.add(isBlank(object) ? null : object);       types.add(Types.VARCHAR);

        String sql = "SELECT * FROM " + functionName + "(?, ?, ?, ?, ?)";
        log.debug("EXEC SQL: {} | args={}", sql, args);
        return jdbc.query(sql, args.toArray(), toIntArray(types), JSON_AWARE_MAPPER);
    }

    public List<Map<String, Object>> getDataForAssociation(
            String functionName,
            String object,
            String additionalParam,
            Long limit,
            Long offset,
            String orderBy,
            String where
    ) {
        assertIdentifierSafe(functionName);

        String whereExpr = stripLeading(where, "WHERE");
        String orderExpr = stripLeading(orderBy, "ORDER BY");

        Integer lim = (limit == null) ? null : Math.toIntExact(limit);
        Integer off = (offset == null) ? null : Math.toIntExact(offset);

        List<Object> args  = new ArrayList<>(6);
        List<Integer> types = new ArrayList<>(6);


        args.add(lim);  types.add(Types.INTEGER);
        args.add(off);  types.add(Types.INTEGER);
//        if (arrayName != null) {args.add(arrayName); types.add(Types.VARCHAR);}
        if (whereExpr != null || !whereExpr.isBlank()) {args.add(whereExpr); types.add(Types.VARCHAR);}
        if (orderExpr != null || !orderExpr.isBlank()) {args.add(orderExpr); types.add(Types.VARCHAR);}
        if (object != null) {args.add(object); types.add(Types.VARCHAR);}
        if (additionalParam != null) {args.add(additionalParam); types.add(Types.VARCHAR);}

        StringBuilder argsStr = new StringBuilder(" (");
        for(int i = 0; i < args.size(); i++){
            if(i == args.size() - 1){
                argsStr.append("?)");
                break;
            }
            argsStr.append("?, ");
        }

        String sql = "SELECT * FROM " + functionName + argsStr.toString();
        log.debug("EXEC SQL: {} | args={}", sql, args);
        return jdbc.query(sql, args.toArray(), toIntArray(types), JSON_AWARE_MAPPER);
    }

    /** Перегрузка с p_object и Clause: инлайн внутри репо */
    public List<Map<String, Object>> getDataFromFunction(
            String functionName,
            SqlUtil.Clause whereClause,
            String orderBy,
            Long limit,
            Long offset,
            String object
    ) {
        String inlined = (whereClause == null) ? "" : SqlUtil.inlineWhereForFunction(whereClause);
        return getDataFromFunction(functionName, inlined, orderBy, limit, offset, object);
    }

    /**
     * SELECT * FROM table [WHERE ...] [ORDER BY ...] [LIMIT ?] [OFFSET ?]
     */
    public List<Map<String, Object>> getDataFromTable(
            String tableOrView,
            String whereClause,
            String orderBy,
            Long limit,
            Long offset
    ) {
        assertIdentifierSafe(tableOrView);

        String whereExpr = stripLeading(whereClause, "WHERE");
        String orderExpr = stripLeading(orderBy, "ORDER BY");

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableOrView);
        List<Object> args = new ArrayList<>(2);
        List<Integer> types = new ArrayList<>(2);

        if (!isBlank(whereExpr)) {
            sql.append(" WHERE ").append(whereExpr);
        }
        if (!isBlank(orderExpr)) {
            sql.append(" ORDER BY ").append(orderExpr);
        }
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit);
            types.add(Types.BIGINT);
        }
        if (offset != null) {
            sql.append(" OFFSET ?");
            args.add(offset);
            types.add(Types.BIGINT);
        }

        return args.isEmpty()
                ? jdbc.query(sql.toString(), JSON_AWARE_MAPPER)
                : jdbc.query(sql.toString(), args.toArray(), toIntArray(types), JSON_AWARE_MAPPER);
    }

    /* ----------------- НОВОЕ: вызов функции с четырьмя параметрами (one row) ----------------- */

    /**
     * Универсальный вызов функции с четырьмя параметрами:
     * (limit BIGINT, offset BIGINT, where TEXT, orderBy TEXT)
     * Попутно исправлена прежняя опечатка типов: первые два параметра теперь BIGINT, а не VARCHAR.
     */
    public List<Map<String, Object>> getOneFromFunction(
            String functionName,
            Long limit, Long offset,
            String whereParam, String orderParam) {

        assertIdentifierSafe(functionName);

        String sql = "SELECT * FROM " + functionName + "(?, ?, ?, ?)";
        Object[] args = new Object[] { limit, offset, safeString(whereParam), safeString(orderParam) };
        int[] types  = new int[] { Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.VARCHAR };

        if (log.isDebugEnabled()) {
            log.debug("EXEC ONE SQL: {} | args=[{}, {}, {}, {}]",
                    sql, limit, offset, safeString(whereParam), safeString(orderParam));
        }

        List<Map<String, Object>> rows = jdbc.query(sql, args, types, JSON_AWARE_MAPPER);
        return rows.isEmpty() ? new ArrayList<>() : rows;
    }

    public List<Map<String, Object>> getOneFromFunction(String functionName, String p1, String p2, String p3) {
        assertIdentifierSafe(functionName);

        String sql = "SELECT * FROM " + functionName + "(?, ?, ?)";
        Object[] args = new Object[] { safeString(p1), safeString(p2), safeString(p3) };
        int[] types  = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };

        if (log.isDebugEnabled()) {
            log.debug("EXEC ONE SQL: {} | args=[{}, {}, {}]", sql, safeString(p1), safeString(p2), safeString(p3));
        }

        List<Map<String, Object>> rows = jdbc.query(sql, args, types, JSON_AWARE_MAPPER);
        return rows.isEmpty() ? new ArrayList<>() : rows;
    }

    public Map<String, Object> getOneFromFunction(String functionName, String p1, String p2) {
        assertIdentifierSafe(functionName);

        String sql = "SELECT * FROM " + functionName + "(?, ?)";
        Object[] args = new Object[] { safeString(p1), safeString(p2) };
        int[] types  = new int[] { Types.VARCHAR, Types.VARCHAR };

        if (log.isDebugEnabled()) {
            log.debug("EXEC ONE SQL: {} | args=[{}, {}]", sql, safeString(p1), safeString(p2));
        }

        List<Map<String, Object>> rows = jdbc.query(sql, args, types, JSON_AWARE_MAPPER);
        return rows.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(rows.get(0));
    }

    public List<Map<String, Object>> getOneFromFunction(String functionName, String p1) {
        assertIdentifierSafe(functionName);
        String sql = "SELECT * FROM " + functionName + "(?)";
        Object[] args = new Object[] { safeString(p1) };
        int[] types  = new int[] { Types.VARCHAR };

        if (log.isDebugEnabled()) {
            log.debug("EXEC ONE SQL: {} | args=[{}]", sql, safeString(p1));
        }
        List<Map<String, Object>> rows = jdbc.query(sql, args, types, JSON_AWARE_MAPPER);
        return rows.isEmpty() ? new ArrayList<>() : rows;
    }

    /* ----------------- helpers ----------------- */

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safeString(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String stripLeading(String s, String keyword) {
        if (isBlank(s)) return s;
        String trimmed = s.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String kw = keyword.toLowerCase(Locale.ROOT);
        if (lower.startsWith((kw + " "))) {
            return trimmed.substring(keyword.length()).trim();
        }
        return trimmed;
    }

    /** Жёсткая проверка идентификатора (функция/таблица) — только [a-zA-Z0-9_.] */
    private static void assertIdentifierSafe(String id) {
        if (isBlank(id) || !id.matches("[a-zA-Z0-9_\\.]+")) {
            throw new IllegalArgumentException("Bad identifier: " + id);
        }
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
        return a;
    }
}
