package org.example.util;

import org.example.model.filter.FilterNode;
import org.example.util.filter.FilterSqlBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Строит запрос:
 *   inner:    SELECT * FROM <tableOrSubquery> [WHERE base AND/OR innerFilters]
 *   outer:    SELECT * FROM (inner) sub [WHERE outerFilters]
 *   source:   (опц.) SELECT x.*, COUNT(*) OVER() AS total_count FROM (outer) x
 *   page:     SELECT * FROM (source) page [ORDER BY ..] [LIMIT ? OFFSET ?]
 *
 * Порядок params:
 *   1) baseWhere params
 *   2) innerFilters params
 *   3) outerFilters params
 *   4) LIMIT, OFFSET
 */
public class SqlBuilder {

    // --- Конфигурация источника ---
    private String tableName;                    // имя таблицы или "(SELECT ... ) t"
    private String baseWhereCondition;           // внутренняя WHERE (фиксированная)
    private final List<Object> baseParams = new ArrayList<>();

    // --- Фильтры ---
    public enum FilterPlacement { INNER, OUTER }

    private FilterNode innerFilterNode;
    private Function<String,String> innerResolver = Function.identity();

    private FilterNode outerFilterNode;
    private Function<String,String> outerResolver = Function.identity();

    // --- ORDER/LIMIT/OFFSET ---
    private final List<String> orderByClauses = new ArrayList<>();
    private Integer limit;
    private Integer offset;

    // --- Окно total_count ---
    private boolean withWindowTotal = false;

    /** Источник данных: имя таблицы или подзапрос "(SELECT ... ) t" */
    public static SqlBuilder selectFrom(String tableOrSubquery) {
        SqlBuilder b = new SqlBuilder();
        b.tableName = tableOrSubquery;
        return b;
    }

    /** Внутренний статичный WHERE */
    public SqlBuilder where(String condition, List<Object> params) {
        if (condition != null && !condition.isBlank()) {
            this.baseWhereCondition = "(" + condition + ")";
            if (params != null && !params.isEmpty()) this.baseParams.addAll(params);
        }
        return this;
    }

    /** Фильтры: куда их применяем — ВНУТРЬ или СНАРУЖИ */
    public SqlBuilder filter(List<FilterNode> filters,
                             Function<String,String> resolver,
                             FilterPlacement placement) {
        if (filters == null || filters.isEmpty()) return this;

        FilterNode node = (filters.size() == 1)
                ? filters.get(0)
                : new FilterNode("and", filters, null, null, null, null, null, null, null);

        if (placement == FilterPlacement.INNER) {
            this.innerFilterNode = node;
            this.innerResolver   = (resolver != null) ? resolver : Function.identity();
        } else {
            this.outerFilterNode = node;
            this.outerResolver   = (resolver != null) ? resolver : Function.identity();
        }
        return this;
    }

    /** Упрощённые перегрузки */
    public SqlBuilder filterInner(List<FilterNode> filters, Function<String,String> resolver) {
        return filter(filters, resolver, FilterPlacement.INNER);
    }
    public SqlBuilder filterOuter(List<FilterNode> filters, Function<String,String> resolver) {
        return filter(filters, resolver, FilterPlacement.OUTER);
    }
    public SqlBuilder filterInner(List<FilterNode> filters) {
        return filter(filters, Function.identity(), FilterPlacement.INNER);
    }
    public SqlBuilder filterOuter(List<FilterNode> filters) {
        return filter(filters, Function.identity(), FilterPlacement.OUTER);
    }

    /** ORDER BY по "простым" именам (санитайз) */
    public SqlBuilder orderBy(Sort sort) {
        if (sort != null && sort.isSorted()) {
            this.orderByClauses.addAll(
                    sort.stream()
                            .map(o -> sanitize(o.getProperty()) + " " + o.getDirection())
                            .collect(Collectors.toList())
            );
        }
        return this;
    }

    /** ORDER BY с резолвером (ответственность за безопасность выражений на вызывающем коде) */
    public SqlBuilder order(Sort sort, Function<String,String> resolver) {
        if (sort != null && sort.isSorted()) {
            this.orderByClauses.addAll(
                    sort.stream()
                            .map(o -> {
                                String expr = (resolver != null)
                                        ? resolver.apply(o.getProperty())
                                        : sanitize(o.getProperty());
                                return expr + " " + o.getDirection();
                            })
                            .collect(Collectors.toList())
            );
        }
        return this;
    }

    /** Пагинация + сортировка с резолвером */
    public SqlBuilder paginate(Pageable pageable, Function<String,String> resolver) {
        if (pageable != null && pageable.isPaged()) {
            this.limit  = pageable.getPageSize();
            this.offset = (int) pageable.getOffset();
            this.order(pageable.getSort(), resolver);
        }
        return this;
    }

    /** Пагинация + сортировка без резолвера */
    public SqlBuilder paginate(Pageable pageable) {
        if (pageable != null && pageable.isPaged()) {
            this.limit  = pageable.getPageSize();
            this.offset = (int) pageable.getOffset();
            this.orderBy(pageable.getSort());
        }
        return this;
    }

    /** Включить COUNT(*) OVER() AS total_count (один запрос) */
    public SqlBuilder withWindowTotal() {
        this.withWindowTotal = true;
        return this;
    }

    /** Классический COUNT (оставлен, если вдруг понадобится) */
    public SqlPageQuery count() {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();

        // inner base
        sql.append("SELECT COUNT(*) FROM (SELECT * FROM ").append(tableName);
        if (hasBaseWhere()) {
            sql.append(" WHERE ").append(baseWhereCondition);
            params.addAll(baseParams);
        }

        // inner + innerFilters
        if (innerFilterNode != null) {
            var f = FilterSqlBuilder.newBuilder()
                    .withFieldResolver(innerResolver)
                    .buildWhere(innerFilterNode);
            sql.append(hasBaseWhere() ? " AND (" : " WHERE (")
                    .append(f.sql()).append(")");
            params.addAll(f.params());
        }
        sql.append(") AS sub");

        // outer filters
        if (outerFilterNode != null) {
            var f = FilterSqlBuilder.newBuilder()
                    .withFieldResolver(outerResolver)
                    .buildWhere(outerFilterNode);
            sql.append(" WHERE ").append(f.sql());
            params.addAll(f.params());
        }

        return new SqlPageQuery(sql.toString(), params);
    }

    /** Итоговый SELECT c optional total_count OVER() */
    public SqlPageQuery build() {
        List<Object> params = new ArrayList<>();
        StringBuilder sql   = new StringBuilder();

        // 1) inner: base + innerFilters (фильтры применяются РАНО, до окон/лимитов)
        StringBuilder inner = new StringBuilder("SELECT * FROM ").append(tableName);
        if (hasBaseWhere()) {
            inner.append(" WHERE ").append(baseWhereCondition);
            params.addAll(baseParams);
        }
        if (innerFilterNode != null) {
            var f = FilterSqlBuilder.newBuilder()
                    .withFieldResolver(innerResolver)
                    .buildWhere(innerFilterNode);
            inner.append(hasBaseWhere() ? " AND (" : " WHERE (")
                    .append(f.sql()).append(")");
            params.addAll(f.params());
        }

        // 2) outer: накатываем внешние фильтры (если нужны)
        StringBuilder outer = new StringBuilder("SELECT * FROM (")
                .append(inner)
                .append(") sub");
        if (outerFilterNode != null) {
            var f = FilterSqlBuilder.newBuilder()
                    .withFieldResolver(outerResolver)
                    .buildWhere(outerFilterNode);
            outer.append(" WHERE ").append(f.sql());
            params.addAll(f.params());
        }

        // 3) source: с/без оконного total_count
        String source = withWindowTotal
                ? "SELECT x.*, COUNT(*) OVER() AS total_count FROM (" + outer + ") x"
                : outer.toString();

        // 4) page: финальный слой с ORDER/LIMIT/OFFSET
        sql.append("SELECT * FROM (").append(source).append(") page");

        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderByClauses));
        }
        if (limit != null && offset != null) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);
        }

        return new SqlPageQuery(sql.toString(), params);
    }

    // --- helpers ---

    public record SqlPageQuery(String sql, List<Object> params) {}

    private boolean hasBaseWhere() {
        return baseWhereCondition != null && !baseWhereCondition.isBlank();
    }

    private static String sanitize(String column) {
        return column.replaceAll("[^a-zA-Z0-9_]", "");
    }

    public static String parameterize(String sqlQuery, Object param){

        return sqlQuery.replace("?", "'" + param.toString() + "'");
    }

    public record SqlFilter(String whereClause, List<Object> params) {}


}
