package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.RoleProfileExtDto;
import org.example.model.filter.FilterNode;
import org.example.util.TinySql;
import org.example.util.filter.FilterSqlBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoleProfileExtJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    // ВЕРХНИЙ SELECT — БЕЗ WHERE (фильтр добавим внутрь через TinySql.oneShot)
    private static final String BASE_SELECT = """
        (
            SELECT row_number() OVER () AS id,
                   m_role.oid AS oid,
                   xx.value AS ext_attr_value,
                   replace(mei.itemname, glt_get_config_value('shema\\ext_shema'), '') AS ext_attr_name,
                   mei.cardinality,
                   split_part(mei.valuetype, '#', 2) AS type,
                   CASE
                       WHEN mei.valuetype LIKE (glt_get_config_value('shema\\ext_shema') || '%') THEN 'EXT'
                       ELSE 'BASIC'
                   END AS contanertype
            FROM m_role
            LEFT JOIN LATERAL json_each_text(m_role.ext::json) xx(key, value) ON true
            LEFT JOIN m_ext_item mei ON mei.id = xx.key::integer
        ) AS base
        """;

    // --- Резолвер полей фильтра: алиас/DTO → выражение "base.<column>" ---
    private static String norm(String s) { return s == null ? null : s.replace("_", "").toLowerCase(); }

    // перечисляем колонки, которые есть в верхнем SELECT (алиасы)
    private static final List<String> COLS = List.of(
            "id", "oid", "ext_attr_value", "ext_attr_name", "cardinality", "type", "contanertype"
    );
    private static final Map<String,String> FIELD_TO_EXPR = COLS.stream()
            .collect(Collectors.toMap(c -> norm(c), c -> "base." + c));

    private String resolveField(String field) {
        String key = norm(field);
        String expr = FIELD_TO_EXPR.get(key);
        if (expr != null) return expr;

        // поддержка camelCase DTO-имен
        if ("extattrvalue".equals(key)) return "base.ext_attr_value";
        if ("extattrname".equals(key))  return "base.ext_attr_name";
        if ("containertype".equals(key)) return "base.contanertype"; // в SELECT алиас именно contanertype

        // fallback — безопасно очищаем и префиксуем
        String cleaned = field == null ? "" : field.replaceAll("[^A-Za-z0-9_]", "");
        return "base." + cleaned;
    }

    // --- Публичный метод: один запрос с COUNT OVER, log.debug SQL/params ---
    public Page<RoleProfileExtDto> findAll(Pageable pageable, List<FilterNode> filters) {
        // 1) WHERE внутрь BASE по реальным выражениям
        var where = FilterSqlBuilder.newBuilder()
                .withFieldResolver(this::resolveField)
                .buildWhere(filters);

        // 2) Один SQL: COUNT(*) OVER() + ORDER BY (по алиасам) + LIMIT/OFFSET
        var q = TinySql.oneShot(
                BASE_SELECT,
                where.sql(),
                where.params(),
                false,              // верхний SELECT без WHERE → вставляем WHERE (...)
                pageable
        );

        log.debug("[RoleProfileExtJdbcRepository] SQL:\n{}\nPARAMS: {}", q.sql(), q.params());

        // 3) Одна выборка: достаём total_count из первой строки и мапим DTO
        AtomicLong totalHolder = new AtomicLong(0);
        var mapper = new BeanPropertyRowMapper<>(RoleProfileExtDto.class);

        List<RoleProfileExtDto> content = jdbcTemplate.query(q.sql(), q.params().toArray(), (ResultSet rs) -> {
            List<RoleProfileExtDto> list = new ArrayList<>();
            int rowNum = 0;
            while (rs.next()) {
                if (rowNum == 0) totalHolder.set(rs.getLong("total_count"));
                list.add(mapper.mapRow(rs, rowNum));
                rowNum++;
            }
            return list;
        });

        long total = totalHolder.get();
        return new PageImpl<>(content, pageable, total);
    }
}
