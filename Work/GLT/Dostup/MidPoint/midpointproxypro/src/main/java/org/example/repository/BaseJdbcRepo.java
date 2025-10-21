// org.example.repository.JdbcRepository
package org.example.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.util.TinySql;
import org.example.util.filter.FilterSqlBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public abstract class BaseJdbcRepo<T> {

    protected final JdbcTemplate jdbc;

    protected abstract String baseSelect();
    protected abstract boolean baseHasWhere();
    protected abstract Function<String,String> fieldResolver();
    protected abstract T mapRow(Map<String,Object> row);

    public Page<T> page(List<FilterNode> filters, Pageable pageable) {
        var where = FilterSqlBuilder.newBuilder()
                .withFieldResolver(fieldResolver())
                .buildWhere(filters);

        var q = TinySql.oneShot(
                baseSelect(),
                where.sql(),
                where.params(),
                baseHasWhere(),
                pageable
        );

        log.debug("SQL:\n{}\nPARAMS: {}", q.sql(), q.params());

        List<Map<String,Object>> rows = jdbc.queryForList(q.sql(), q.params().toArray());
        long total = stripTotal(rows);
        List<T> mapped = rows.stream().map(this::mapRow).toList();
        return new PageImpl<>(mapped, pageable, total);
    }

    protected long stripTotal(List<Map<String,Object>> rows) {
        if (rows == null || rows.isEmpty()) return 0L;
        Object v = rows.get(0).get("total_count");
        long total = (v instanceof Number n) ? n.longValue() : 0L;
        rows.forEach(m -> m.remove("total_count"));
        return total;
    }
}
