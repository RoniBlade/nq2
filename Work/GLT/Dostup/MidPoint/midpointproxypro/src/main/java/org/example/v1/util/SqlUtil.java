// SqlUtil.java
package org.example.v1.util;

import org.example.model.filter.FilterCriterion;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SqlUtil {

    private static final int DEFAULT_LIMIT = 50;
    private SqlUtil() {}

    private static final Pattern SAFE_IDENT =
            Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*");

    /** Универсальный фрагмент SQL с параметрами */
    public record Clause(String sql, List<Object> params) {
        public Object[] args() { return params.toArray(); }
        public String withPrefix(String prefix) {
            return (sql == null || sql.isBlank()) ? "" : prefix + " " + sql;
        }
    }

    // ---------------- WHERE ----------------

    public static String getSqlWhere(List<FilterNode> nodes) {
        return toWhere(nodes).withPrefix("WHERE");
    }

    public static Clause toWhere(List<FilterNode> nodes) {
        return toWhere(nodes, "and", null);
    }

    public static Clause toWhere(List<FilterNode> nodes, String logic) {
        return toWhere(nodes, logic, null);
    }

    public static Clause toWhere(List<FilterNode> nodes, String logic, Map<String,String> fieldMap) {
        if (nodes == null || nodes.isEmpty()) return new Clause("", List.of());

        List<Object> params = new ArrayList<>();
        String glue = "or".equalsIgnoreCase(logic) ? " OR " : " AND ";

        List<String> parts = new ArrayList<>();
        for (FilterNode n : nodes) {
            String s = build(n, params, fieldMap);
            if (s != null && !s.isBlank()) parts.add("(" + s + ")");
        }
        String sql = parts.isEmpty() ? "" : String.join(glue, parts);
        return new Clause(sql, params);
    }

    public static String getSqlWhere(FilterNode node) {
        return toWhere(node).withPrefix("WHERE");
    }

    public static Clause toWhere(FilterNode node) {
        return toWhere(node, null);
    }

    public static Clause toWhere(FilterNode node, Map<String,String> fieldMap) {
        List<Object> params = new ArrayList<>();
        String expr = build(node, params, fieldMap);
        return new Clause(expr == null ? "" : expr, params);
    }

    private static String build(FilterNode n, List<Object> params, Map<String,String> fieldMap) {
        if (n == null) return "";
        if (n.isLeaf()) return render(n.getCriterion(), params, fieldMap);

        List<FilterNode> kids = n.getFilters();
        if (kids == null || kids.isEmpty()) return "";

        String glue = "or".equalsIgnoreCase(n.getLogic()) ? " OR " : " AND ";

        List<String> parts = kids.stream()
                .map(child -> build(child, params, fieldMap))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());

        if (parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.get(0);
        return parts.stream().map(s -> "(" + s + ")").collect(Collectors.joining(glue));
    }

    private static String render(FilterCriterion c, List<Object> params, Map<String,String> fieldMap) {
        if (c == null) return "";
        String col = resolveField(c.getField(), fieldMap);
        FilterOperation op = c.getOp();
        Object val = c.getValue();
        if (op == null) throw new IllegalArgumentException("op is null for field: " + c.getField());

        switch (op) {
            case EQUAL:
                if (val == null) return col + " IS NULL";
                params.add(val); return col + " = ?";
            case NOT_EQUAL:
                if (val == null) return col + " IS NOT NULL";
                params.add(val); return col + " <> ?";

            case GREATER_THAN:     params.add(val); return col + " > ?";
            case LESS_THAN:        params.add(val); return col + " < ?";
            case GREATER_OR_EQUAL: params.add(val); return col + " >= ?";
            case LESS_OR_EQUAL:    params.add(val); return col + " <= ?";

            case IN: {
                List<?> list = toList(val);
                if (list.isEmpty()) return "1=0";
                params.addAll(list);
                return col + " IN (" + placeholders(list.size()) + ")";
            }
            case NOT_IN: {
                List<?> list = toList(val);
                if (list.isEmpty()) return "1=1";
                params.addAll(list);
                return col + " NOT IN (" + placeholders(list.size()) + ")";
            }

            case SUBSTRING:      params.add("%" + asString(val) + "%"); return col + " ILIKE ?";
            case NOT_SUBSTRING:  params.add("%" + asString(val) + "%"); return col + " NOT ILIKE ?";
            case STARTS_WITH:    params.add(asString(val) + "%");       return col + " ILIKE ?";
            case ENDS_WITH:      params.add("%" + asString(val));       return col + " ILIKE ?";

            case NOT_NULL:  return col + " IS NOT NULL";
            case IS_NULL:   return col + " IS NULL";

            default:
                throw new UnsupportedOperationException("Unsupported op: " + op);
        }
    }

    /** 1) если есть в fieldMap — берём; 2) иначе — проверяем, что имя безопасное */
    private static String resolveField(String apiField, Map<String,String> fieldMap) {
        if (apiField == null || apiField.isBlank())
            throw new IllegalArgumentException("Empty field name");
        if (fieldMap != null) {
            String mapped = fieldMap.get(apiField);
            if (mapped != null && !mapped.isBlank()) return mapped;
        }
        if (SAFE_IDENT.matcher(apiField).matches()) return apiField;
        throw new IllegalArgumentException(
                "Unsafe field name: " + apiField + " (разрешены alias.column / schema.table.column или явный маппинг)"
        );
    }

    // ---------------- ORDER BY ----------------

    public static String toOrderBy(Pageable pageable) {
        if (pageable == null) return "";
        return toOrderBy(pageable.getSort(), null);
    }

    public static String toOrderBy(Pageable pageable, Map<String,String> fieldMap) {
        if (pageable == null) return "";
        return toOrderBy(pageable.getSort(), fieldMap);
    }

    public static String toOrderBy(Sort sort) {
        return toOrderBy(sort, null);
    }

    public static String toOrderBy(Sort sort, Map<String,String> fieldMap) {
        if (sort == null || sort.isUnsorted()) return "";
        List<String> parts = new ArrayList<>();
        for (Sort.Order o : sort) {
            String col = resolveField(o.getProperty(), fieldMap);
            String expr = o.isIgnoreCase() ? "LOWER(" + col + ")" : col;
            String dir = (o.getDirection() == Sort.Direction.DESC) ? "DESC" : "ASC";
            String nulls = switch (o.getNullHandling()) {
                case NULLS_FIRST -> " NULLS FIRST";
                case NULLS_LAST  -> " NULLS LAST";
                default -> "";
            };
            parts.add(expr + " " + dir + nulls);
        }
        return parts.isEmpty() ? "" : "ORDER BY " + String.join(", ", parts);
    }

    // ---------------- LIMIT/OFFSET ----------------

    public static Clause toLimitOffset(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) return new Clause("", List.of());
        return new Clause("LIMIT ? OFFSET ?", List.of(pageable.getPageSize(), pageable.getOffset()));
    }

    /** Возвращает только число LIMIT: 1 <= limit <= maxLimit; unpaged → defaultLimit */
    public static long limit(Pageable pageable) {
        int lim = DEFAULT_LIMIT;
        if (pageable != null && !pageable.isUnpaged()) lim = pageable.getPageSize();
        if (lim <= 0) lim = DEFAULT_LIMIT;
        return (long) lim;
    }

    /** Возвращает только число OFFSET; unpaged → 0 */
    public static long offset(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) return 0L;
        long off = pageable.getOffset();
        return Math.max(0L, off);
    }

    // ---------------- inline WHERE для функций ----------------

    /** Инлайнит параметры из Clause в текст WHERE (без префикса WHERE). */
    public static String inlineWhereForFunction(Clause whereClause) {
        if (whereClause == null || whereClause.sql() == null || whereClause.sql().isBlank()) return "";
        String sql = whereClause.sql();
        List<Object> params = whereClause.params();
        int qCount = countPlaceholders(sql);
        if (qCount != params.size()) {
            throw new IllegalArgumentException("Количество параметров (" + params.size() +
                    ") не совпадает с числом плейсхолдеров '?' (" + qCount + ") в [" + sql + "]");
        }

        StringBuilder out = new StringBuilder(sql.length() + 32);
        int pi = 0;
        boolean inStr = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                out.append(ch);
                // handle escaped quotes: ''
                if (inStr && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    out.append('\'');
                    i++;
                } else {
                    inStr = !inStr;
                }
            } else if (ch == '?' && !inStr) {
                out.append(toSqlLiteral(params.get(pi++)));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static int countPlaceholders(String sql) {
        boolean inStr = false;
        int cnt = 0;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                if (inStr && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i++; // skip escaped '
                } else {
                    inStr = !inStr;
                }
            } else if (ch == '?' && !inStr) {
                cnt++;
            }
        }
        return cnt;
    }

    private static String toSqlLiteral(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        if (v instanceof UUID u) return "'" + u + "'::uuid";

        // java.time
        if (v instanceof LocalDate d) return "DATE '" + d + "'";
        if (v instanceof LocalDateTime dt) return "TIMESTAMP '" + dt + "'";
        if (v instanceof OffsetDateTime odt) return "'"
                + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "'::timestamptz";
        if (v instanceof ZonedDateTime zdt) return "'"
                + zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "'::timestamptz";
        if (v instanceof Instant inst) return "'" + inst.toString() + "'::timestamptz";

        // java.sql
        if (v instanceof java.sql.Date d) return "DATE '" + d.toLocalDate() + "'";
        if (v instanceof java.sql.Timestamp ts) return "TIMESTAMP '" + ts.toLocalDateTime() + "'";
        if (v instanceof java.sql.Time t) return "'" + t.toString() + "'::time";

        if (v instanceof Enum<?> e) return quote(e.name());

        // всё остальное — как строка
        return quote(v.toString());
    }

    private static String quote(String s) {
        String esc = (s == null) ? "" : s.replace("'", "''");
        return "'" + esc + "'";
    }

    // ---------------- helpers ----------------

    private static List<?> toList(Object v) {
        if (v == null) return List.of();
        if (v instanceof Collection<?> c) return new ArrayList<>(c);
        if (v.getClass().isArray()) {
            int n = java.lang.reflect.Array.getLength(v);
            List<Object> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(java.lang.reflect.Array.get(v, i));
            return out;
        }
        if (v instanceof String s && s.contains(",")) {
            return Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
        }
        return List.of(v);
    }

    private static String placeholders(int n) { return String.join(", ", Collections.nCopies(n, "?")); }
    private static String asString(Object v) { return v == null ? "" : v.toString(); }
}
