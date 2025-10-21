package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.UserProfileExtDto;
import org.example.model.filter.FilterNode;
import org.example.util.filter.FilterSqlBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserProfileExtJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public Page<UserProfileExtDto> findAll(Pageable pageable, List<FilterNode> filterNodes) {
        UUID oid = null;
        List<FilterNode> cleaned = new ArrayList<>();

        if (filterNodes != null) {
            for (FilterNode node : filterNodes) {
                if (node.isLeaf()
                        && "oid".equalsIgnoreCase(node.getCriterion().getField())
                        && node.getCriterion().getValue() != null) {
                    oid = UUID.fromString(node.getCriterion().getValue().toString());
                } else {
                    cleaned.add(node);
                }
            }
        }

        String baseSql = """
            SELECT
                row_number() OVER () AS id,
                m_user.oid AS oid,
                xx.value AS ext_attr_value,
                replace(mei.itemname, glt_get_config_value('shema\\ext_shema'), '') AS ext_attr_name,
                mei.cardinality,
                split_part(mei.valuetype, '#', 2) AS type,
                CASE
                    WHEN mei.valuetype LIKE (glt_get_config_value('shema\\ext_shema') || '%') THEN 'EXT'
                    ELSE 'BASIC'
                END AS contaner_type
            FROM m_user
                LEFT JOIN LATERAL json_each_text(m_user.ext::json) xx(key, value) ON true
                LEFT JOIN m_ext_item mei ON mei.id = xx.key::integer
            """;

        List<FilterSqlBuilder.SqlFilter> sqlFilters = cleaned.stream()
                .map(FilterSqlBuilder::fromNode)
                .filter(f -> f != null && f.sql() != null && !f.sql().isBlank())
                .map(f -> {
                    String updated = f.sql()
                            .replaceAll("\\boid\\s*=\\s*\\?", "m_user.oid = ?::uuid")
                            .replaceAll("\\boid\\s*!=\\s*\\?", "m_user.oid != ?::uuid")
                            .replaceAll("\\boid\\s+IN\\s*\\(\\s*\\?\\s*\\)", "m_user.oid IN (?::uuid[])");
                    return new FilterSqlBuilder.SqlFilter(updated, f.params());
                })
                .toList();

        String whereClause = sqlFilters.stream()
                .map(FilterSqlBuilder.SqlFilter::sql)
                .collect(Collectors.joining(" AND "));

        List<Object> allParams = new ArrayList<>();
        if (oid != null) {
            whereClause = (whereClause.isBlank() ? "" : whereClause + " AND ") + "m_user.oid = ?::uuid";
            allParams.add(oid);
        }
        for (FilterSqlBuilder.SqlFilter f : sqlFilters) {
            allParams.addAll(f.params());
        }

        String fullWhere = whereClause.isBlank() ? "" : " WHERE " + whereClause;

        String countSql = "SELECT COUNT(*) FROM (" + baseSql + fullWhere + ") AS total";
        long total = jdbcTemplate.queryForObject(countSql, allParams.toArray(), Long.class);

        String sort = pageable != null && !pageable.getSort().isEmpty()
                ? pageable.getSort().stream()
                .map(o -> "sub." + o.getProperty() + " " + o.getDirection())
                .collect(Collectors.joining(", ", " ORDER BY ", ""))
                : " ORDER BY sub.oid";

        String pagedSql = "SELECT * FROM (" + baseSql + fullWhere + ") AS sub" + sort;

        if (pageable != null && pageable.isPaged()) {
            pagedSql += " LIMIT ? OFFSET ?";
            allParams.add(pageable.getPageSize());
            allParams.add(pageable.getOffset());
        }

        List<UserProfileExtDto> result = jdbcTemplate.query(
                pagedSql,
                allParams.toArray(),
                new BeanPropertyRowMapper<>(UserProfileExtDto.class)
        );

        return new PageImpl<>(result, pageable == null ? Pageable.unpaged() : pageable, total);
    }
}
