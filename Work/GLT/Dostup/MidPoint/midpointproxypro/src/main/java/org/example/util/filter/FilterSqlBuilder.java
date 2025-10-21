package org.example.util.filter;

import org.example.model.filter.FilterCriterion;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterOperation;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterSqlBuilder {

    // ====== ПУБЛИЧНЫЙ API ======

    /** Старый API — остаётся для обратной совместимости (без резолвера алиасов). */
    public static SqlFilter fromNode(FilterNode node) {
        return newBuilder().buildWhere(node);
    }

    /** Новый API — билдим WHERE с возможностью подстановки "алиас -> выражение до AS". */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private Function<String, String> fieldResolver = Function.identity();

        /**
         * Резолвер имени поля: принимает алиас из фильтров и возвращает выражение «до AS»,
         * например: "objectFullName" -> CASE ... END
         */
        public Builder withFieldResolver(Function<String, String> resolver) {
            this.fieldResolver = resolver != null ? resolver : Function.identity();
            return this;
        }

        /** Собирает WHERE из одного узла. */
        public SqlFilter buildWhere(FilterNode node) {
            if (node == null) return new SqlFilter("", List.of());
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            build(node, sql, params, fieldResolver);
            return new SqlFilter(sql.toString(), params);
        }

        /** Собирает WHERE из списка узлов как AND-цепочку. */
        public SqlFilter buildWhere(List<FilterNode> nodes) {
            if (nodes == null || nodes.isEmpty()) return new SqlFilter("", List.of());
            if (nodes.size() == 1) return buildWhere(nodes.get(0));
            FilterNode and = new FilterNode();
            and.setLogic("and");
            and.setFilters(nodes);
            return buildWhere(and);
        }
    }

    // ====== ВНУТРЕННОСТИ ======

    // Явный список полей-UUID (можно расширять)
    private static final Set<String> UUID_FIELDS = Set.of(
            "oid", "ownerOid", "targetOid", "oidApprover", "owneroid",
            "caseoid", "useroid", "requesteroid", "objectoid", "targetoid"
    );

    // Регексы для извлечения "чистого" имени поля из выражений вроде a.b, json->>'ownerOid', "tbl"."oid"
    private static final Pattern JSON_EXTRACT_KEY = Pattern.compile("(?:->>|->)\\s*'?([A-Za-z_][A-Za-z0-9_]*)'?");
    private static final Pattern LAST_SEGMENT = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)$");

    /**
     * Нормализуем имя поля:
     * - снимаем кавычки
     * - если есть JSON-извлечение -> берём ключ
     * - иначе берём последний сегмент после точки
     */
    private static String normalizeFieldName(String raw) {
        if (raw == null) return null;
        String s = raw.replace("\"", "").trim();

        // JSON path: tbl.col ->> 'ownerOid'  =>  ownerOid
        Matcher j = JSON_EXTRACT_KEY.matcher(s);
        if (j.find()) {
            return j.group(1);
        }

        // Последний сегмент после точки: a.b.c  =>  c
        Matcher m = LAST_SEGMENT.matcher(s);
        if (m.find()) {
            return m.group(1);
        }
        return s;
    }

    private static boolean isUuidField(String fieldExpr) {
        String name = normalizeFieldName(fieldExpr);
        if (name == null) return false;

        if (UUID_FIELDS.contains(name)) return true;

        // Эвристики по имени
        String lower = name.toLowerCase();
        return lower.equals("oid")
                || lower.endsWith("_oid")
                || name.endsWith("Oid"); // camelCase вариант
    }

    private static String paramToken(String fieldExpr) {
        // для uuid полей используем ?::uuid, иначе обычный ?
        return isUuidField(fieldExpr) ? "?::uuid" : "?";
    }

    private static String nullTokenForIn(String fieldExpr) {
        // чтобы не было type ambiguity: NULL::uuid для uuid-полей
        return isUuidField(fieldExpr) ? "NULL::uuid" : "NULL";
    }

    private static void build(FilterNode node, StringBuilder sql, List<Object> params,
                              Function<String, String> fieldResolver) {
        if (node.isLeaf()) {
            FilterCriterion c = node.getCriterion();
            String fieldRaw = c.getField();
            String field = fieldResolver.apply(fieldRaw); // <<< алиас -> выражение до AS
            FilterOperation op = c.getOp();
            Object value = c.getValue();

            sql.append("(").append(field).append(" ");

            switch (op) {
                case EQUAL -> {
                    sql.append("= ").append(paramToken(field)).append(")");
                    params.add(convertIfNumeric(value));
                }
                case NOT_EQUAL -> {
                    sql.append("!= ").append(paramToken(field)).append(")");
                    params.add(convertIfNumeric(value));
                }
                case GREATER_THAN -> {
                    sql.append("> ").append(paramToken(field)).append(")");
                    params.add(convertIfNumeric(value));
                }
                case GREATER_OR_EQUAL -> {
                    sql.append(">= ").append(paramToken(field)).append(")");
                    params.add(convertIfNumeric(value));
                }
                case LESS_THAN -> {
                    sql.append("< ").append(paramToken(field)).append(")");
                    params.add(convertIfNumeric(value));
                }
                case LESS_OR_EQUAL -> {
                    sql.append("<= ").append(paramToken(field)).append(")");
                    params.add(convertIfNumeric(value));
                }
                case IN, NOT_IN -> {
                    if (!(value instanceof Collection<?> collection) || collection.isEmpty()) {
                        // Пустая коллекция — делаем типобезопасный NULL
                        sql.append(op == FilterOperation.IN
                                ? "IN (" + nullTokenForIn(field) + "))"
                                : "NOT IN (" + nullTokenForIn(field) + "))");
                    } else {
                        sql.append(op == FilterOperation.IN ? "IN (" : "NOT IN (");
                        String token = paramToken(field);
                        sql.append(String.join(", ", Collections.nCopies(collection.size(), token)));
                        sql.append("))");
                        for (Object val : collection) {
                            params.add(convertIfNumeric(val));
                        }
                    }
                }
                case SUBSTRING -> {
                    // текстовые операции — параметр без uuid-каста
                    sql.append("ILIKE ?").append(")");
                    params.add("%" + value + "%");
                }
                case NOT_SUBSTRING -> {
                    sql.append("NOT ILIKE ?").append(")");
                    params.add("%" + value + "%");
                }
                case STARTS_WITH -> {
                    sql.append("ILIKE ?").append(")");
                    params.add(value + "%");
                }
                case ENDS_WITH -> {
                    sql.append("ILIKE ?").append(")");
                    params.add("%" + value);
                }
                default -> throw new UnsupportedOperationException("Unknown operator: " + op);
            }

        } else if (node.getFilters() != null && !node.getFilters().isEmpty()) {
            String logic = (node.getLogic() == null ? "AND" : node.getLogic().toUpperCase());
            sql.append("(");
            for (int i = 0; i < node.getFilters().size(); i++) {
                if (i > 0) sql.append(" ").append(logic).append(" ");
                build(node.getFilters().get(i), sql, params, fieldResolver);
            }
            sql.append(")");
        }
    }

    private static Object convertIfNumeric(Object value) {
        if (value instanceof String str) {
            try {
                // если число без точки — приводим к Long
                if (str.matches("-?\\d+")) {
                    return Long.parseLong(str);
                }
            } catch (NumberFormatException ignored) {}
        }
        return value;
    }

    // Утилита для логов/отладки (не для реального выполнения!)
    public static String symbolsMapper(String sql, Object param){
        if (sql == null) return null;
        if (param instanceof Collection<?> col) {
            String joined = String.join(", ", col.stream().map(Object::toString).toList());
            return sql.replace("?", joined).replace("[", "").replace("]", "");
        }
        if (sql.contains("?")){
            return sql.replace("?", String.valueOf(param)).replace("[", "").replace("]", "");
        }
        return sql;
    }

    // Результат билда WHERE
    public record SqlFilter(String sql, List<Object> params) {}
}
