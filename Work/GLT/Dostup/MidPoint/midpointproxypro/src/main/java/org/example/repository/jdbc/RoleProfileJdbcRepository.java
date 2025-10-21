package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.RoleProfileDto;
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
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoleProfileJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public Page<RoleProfileDto> findAll(Pageable pageable, List<FilterNode> filterNodes) {
        String baseSelectSql = """
            SELECT
                mo.oid,
                mo.nameorig,
                mo.namenorm,
                mo.fullobject,
                ((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'description' AS description,
                ((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'documentation' AS documentation,
                ((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'indestructible' AS indestructible,
                mo.tenantreftargetoid,
                mo.tenantreftargettype,
                mo.tenantrefrelationid,
                mo.lifecyclestate,
                mo.cidseq,
                mo.version,
                mo.policysituations,
                mo.subtypes,
                mo.fulltextinfo,
                mo.ext,
                mo.creatorreftargetoid,
                mo.creatorreftargettype,
                mo.creatorrefrelationid,
                mo.createchannelid,
                mo.createtimestamp,
                mo.modifierreftargetoid,
                mo.modifierreftargettype,
                mo.modifierrefrelationid,
                mo.modifychannelid,
                mo.modifytimestamp,
                mo.db_created AS dbcreated,
                mo.db_modified AS dbmodified,
                mo.objecttype,
                mo.costcenter,
                mo.emailaddress,
                mo.photo,
                mo.locale,
                mo.localityorig,
                mo.localitynorm,
                mo.preferredlanguage,
                mo.telephonenumber,
                mo.timezone,
                mo.passwordcreatetimestamp,
                mo.passwordmodifytimestamp,
                mo.administrativestatus,
                mo.effectivestatus,
                mo.enabletimestamp,
                mo.disabletimestamp,
                mo.disablereason,
                mo.validitystatus,
                mo.validfrom,
                mo.validto,
                mo.validitychangetimestamp,
                mo.archivetimestamp,
                mo.lockoutstatus,
                (((((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'activation')::json) ->> 'lockoutExpirationTimestamp')::timestamp with time zone AS lockoutexpirationtimestamp,
                mo.normalizeddata,
                mo.autoassignenabled,
                mo.displaynameorig,
                mo.displaynamenorm,
                mo.identifier,
                mo.requestable,
                mo.risklevel,
                mo.subtypes AS subtype,
                mo.tenantrefrelationid AS tenantreftargetrelationid,
                ma.oid AS targetarchetypeoid,
                (((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json ->> 'cssClass' AS archetype_icon_class,
                (((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json ->> 'color' AS archetype_icon_color,
                mo.nameorig AS vdisplayname
            FROM m_role mo
                     LEFT JOIN m_archetype ma ON ((((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'archetypeRef')::jsonb ->> 'oid')::uuid = ma.oid
        """;

        List<FilterSqlBuilder.SqlFilter> sqlFilters = (filterNodes == null || filterNodes.isEmpty())
                ? List.of()
                : filterNodes.stream()
                .map(FilterSqlBuilder::fromNode)
                .filter(f -> f != null && f.sql() != null && !f.sql().isBlank())
                .map(f -> {
                    String patched = f.sql()
                            .replaceAll("\\boid\\s*=\\s*\\?", "oid = ?::uuid")
                            .replaceAll("\\boid\\s+IN\\s*\\(\\s*\\?\\s*\\)", "oid IN (?::uuid[])")
                            .replaceAll("\\btargetarchetypeoid\\s*=\\s*\\?", "targetarchetypeoid = ?::uuid")
                            .replaceAll("\\btargetarchetypeoid\\s+IN\\s*\\(\\s*\\?\\s*\\)", "targetarchetypeoid IN (?::uuid[])");
                    return new FilterSqlBuilder.SqlFilter(patched, f.params());
                })
                .toList();

        String whereClause = sqlFilters.isEmpty()
                ? ""
                : " WHERE " + sqlFilters.stream()
                .map(FilterSqlBuilder.SqlFilter::sql)
                .collect(Collectors.joining(" AND "));

        List<Object> allParams = sqlFilters.stream()
                .flatMap(f -> f.params().stream())
                .collect(Collectors.toCollection(ArrayList::new));

        String countSql = "SELECT COUNT(*) FROM (" + baseSelectSql + ") AS base" + (whereClause.isBlank() ? "" : " " + whereClause);
        long total = jdbcTemplate.queryForObject(countSql, allParams.toArray(), Long.class);

        if (pageable == null || pageable.isUnpaged()) {
            String finalSql = "SELECT * FROM (" + baseSelectSql + ") AS base" + whereClause + " ORDER BY base.oid";
            List<RoleProfileDto> content = jdbcTemplate.query(
                    finalSql,
                    allParams.toArray(),
                    new BeanPropertyRowMapper<>(RoleProfileDto.class)
            );
            return new PageImpl<>(content, Pageable.unpaged(), total);
        }

        String sortClause = pageable.getSort().isEmpty()
                ? " ORDER BY sub.oid "
                : " ORDER BY " + pageable.getSort().stream()
                .map(order -> "sub." + order.getProperty() + " " + order.getDirection())
                .collect(Collectors.joining(", "));

        String subQuery = "(SELECT * FROM (" + baseSelectSql + ") AS base " + whereClause + " LIMIT ? OFFSET ?) AS sub";
        allParams.add(pageable.getPageSize());
        allParams.add(pageable.getOffset());

        String finalSql = "SELECT * FROM " + subQuery + sortClause;

        log.info("[RoleProfileJdbcRepository] SQL: {}", finalSql);

        List<RoleProfileDto> content = jdbcTemplate.query(
                finalSql,
                allParams.toArray(),
                new BeanPropertyRowMapper<>(RoleProfileDto.class)
        );

        return new PageImpl<>(content, pageable, total);
    }
}
