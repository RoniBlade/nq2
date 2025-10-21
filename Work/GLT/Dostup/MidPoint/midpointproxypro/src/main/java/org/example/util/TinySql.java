package org.example.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class TinySql {
    private TinySql() {}

    public record Sql(String sql, List<Object> params) {}

    /** Один запрос с COUNT OVER; фильтр встраивается внутрь baseSelect.
     * baseHasWhere = true → добавим AND (...), иначе WHERE (...).
     * Сортировка — по алиасам из baseSelect.
     */
    public static Sql oneShot(String baseSelect,
                              String innerWhereSql,
                              List<Object> innerParams,
                              boolean baseHasWhere,
                              Pageable pageable) {

        String filtered = baseSelect;
        if (innerWhereSql != null && !innerWhereSql.isBlank()) {
            filtered += baseHasWhere
                    ? "\n  AND (" + innerWhereSql + ")"
                    : "\n  WHERE (" + innerWhereSql + ")";
        }

        StringBuilder sql = new StringBuilder()
                .append("SELECT x.*, COUNT(*) OVER() AS total_count ")
                .append("FROM (")
                .append(filtered)
                .append(") x");

        String orderBy = order(pageable);
        if (!orderBy.isBlank()) {
            sql.append(" ORDER BY ").append(orderBy);
        }

        List<Object> params = new ArrayList<>();
        if (innerParams != null) {
            params.addAll(innerParams);
        }

        if (pageable != null && pageable.isPaged()) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(pageable.getPageSize());
            params.add((int) pageable.getOffset());
        }

        return new Sql(sql.toString(), params);
    }

    private static String order(Pageable pageable) {
        if (pageable == null) return "";
        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) return "";
        return sort.stream()
                .map(o -> sanitize(o.getProperty()) + " " + o.getDirection().name())
                .collect(Collectors.joining(", "));
    }

    /** сортируем только по алиасам (без выражений) */
    private static String sanitize(String s) {
        return (s == null) ? "" : s.replaceAll("[^A-Za-z0-9_]", "");
    }
}
