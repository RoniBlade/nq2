package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.UserProfileDto;
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
public class UserProfileJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public Page<UserProfileDto> findAll(Pageable pageable, List<FilterNode> filterNodes) {
        List<FilterNode> cleanedFilters = new ArrayList<>();
        UUID archetypeOid = null;

        if (filterNodes != null) {
            for (FilterNode node : filterNodes) {
                if (node.isLeaf()
                        && "targetarchetypeoid".equalsIgnoreCase(node.getCriterion().getField())
                        && node.getCriterion().getValue() != null) {
                    archetypeOid = UUID.fromString(node.getCriterion().getValue().toString());
                    // не добавляем в список — это условие вырезается
                } else {
                    cleanedFilters.add(node);
                }
            }
            filterNodes = cleanedFilters;
        }

        String baseSelectSql = """
        SELECT
            m_user.oid AS oid,
            m_user.nameorig AS name,
            ((convert_from(m_user.fullobject, 'UTF8')::json ->> 'user')::json) ->> 'description' AS description,
            ((convert_from(m_user.fullobject, 'UTF8')::json) ->> 'documentation') AS documentation,
            ((convert_from(m_user.fullobject, 'UTF8')::json) ->> 'indestructible') AS indestructible,
            m_user.fullobject,
            m_user.tenantreftargetoid,
            m_user.tenantreftargettype,
            m_user.tenantrefrelationid,
            m_user.lifecyclestate,
            m_user.cidseq,
            m_user.version,
            m_user.policysituations,
            m_user.subtypes,
            m_user.fulltextinfo,
            m_user.ext,
            m_user.creatorreftargetoid,
            m_user.creatorreftargettype,
            m_user.creatorrefrelationid,
            m_user.createchannelid,
            m_user.createtimestamp,
            m_user.modifierreftargetoid,
            m_user.modifierreftargettype,
            m_user.modifierrefrelationid,
            m_user.modifychannelid,
            m_user.modifytimestamp,
            m_user.db_created,
            m_user.db_modified,
            m_user.objecttype,
            m_user.costcenter,
            m_user.emailaddress,
            encode(m_user.photo, 'base64') as photo,
            m_user.locale,
            m_user.localityorig,
            m_user.localitynorm,
            m_user.preferredlanguage,
            m_user.telephonenumber,
            m_user.timezone,
            m_user.passwordcreatetimestamp,
            m_user.passwordmodifytimestamp,
            m_user.administrativestatus,
            m_user.effectivestatus,
            m_user.enabletimestamp,
            m_user.disabletimestamp,
            m_user.disablereason,
            m_user.validitystatus,
            m_user.validfrom,
            m_user.validto,
            m_user.validitychangetimestamp,
            m_user.archivetimestamp,
            m_user.lockoutstatus,
            (((((convert_from(m_user.fullobject, 'UTF8')::json ->> 'user')::json) ->> 'activation')::json) ->> 'lockoutExpirationTimestamp')::timestamptz AS lockoutexpirationtimestamp,
            m_user.normalizeddata,
            m_user.additionalnameorig,
            m_user.additionalnamenorm,
            m_user.employeenumber,
            m_user.familynameorig,
            m_user.familynamenorm,
            m_user.fullnameorig,
            m_user.fullnamenorm,
            m_user.givennameorig,
            m_user.givennamenorm,
            m_user.honorificprefixorig,
            m_user.honorificprefixnorm,
            m_user.honorificsuffixorig,
            m_user.honorificsuffixnorm,
            m_user.nicknameorig,
            m_user.nicknamenorm,
            m_user.personalnumber,
            m_user.titleorig,
            m_user.titlenorm,
            m_user.organizations,
            m_user.organizationunits,
            ma.oid AS targetarchetypeoid,
            ma.nameorig AS targetarchetypenameorig,
            (((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json ->> 'cssClass' AS archetype_icon_class,
            (((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json ->> 'color' AS archetype_icon_color,
            CASE WHEN m_user.fullnameorig IS NULL THEN m_user.nameorig ELSE m_user.fullnameorig || ' [' || m_user.nameorig || ']' END AS vdisplayname
        FROM
            m_assignment aa,
            m_user
        LEFT JOIN m_archetype ma ON ((((convert_from(m_user.fullobject, 'UTF8')::json ->> 'user')::json) ->> 'archetypeRef')::jsonb ->> 'oid')::uuid = ma.oid
        WHERE
            aa.ownertype::text = 'USER'
            AND aa.targetreftargettype::text = 'ARCHETYPE'
            AND aa.owneroid = m_user.oid
            AND aa.targetreftargetoid = ma.oid
        """;

        if (archetypeOid != null) {
            baseSelectSql += " AND ma.oid = ?::uuid";
        }

        List<FilterSqlBuilder.SqlFilter> sqlFilters = (filterNodes == null || filterNodes.isEmpty())
                ? List.of()
                : filterNodes.stream()
                .map(FilterSqlBuilder::fromNode)
                .filter(f -> f != null && f.sql() != null && !f.sql().isBlank())
                .map(f -> {
                    String updatedSql = f.sql()
                            .replaceAll("\\boid\\s*=\\s*\\?", "m_user.oid = ?::uuid")
                            .replaceAll("\\boid\\s+IN\\s*\\(\\s*\\?\\s*\\)", "m_user.oid IN (?::uuid[])")
                            .replaceAll("\\boid\\s*!=\\s*\\?", "m_user.oid != ?::uuid")
                            .replaceAll("\\btargetarchetypeoid\\s*=\\s*\\?", "targetarchetypeoid = ?::uuid")
                            .replaceAll("\\btargetarchetypeoid\\s*!=\\s*\\?", "targetarchetypeoid != ?::uuid")
                            .replaceAll("\\btargetarchetypeoid\\s+IN\\s*\\(\\s*\\?\\s*\\)", "targetarchetypeoid IN (?::uuid[])");
                    return new FilterSqlBuilder.SqlFilter(updatedSql, f.params());
                })
                .toList();

        String whereClauseRaw = sqlFilters.stream()
                .map(FilterSqlBuilder.SqlFilter::sql)
                .collect(Collectors.joining(" AND "));
        String whereClause = whereClauseRaw.isBlank() ? "" : " AND " + whereClauseRaw;

        List<Object> allParams = new ArrayList<>();
        if (archetypeOid != null) {
            allParams.add(archetypeOid);
        }
        for (FilterSqlBuilder.SqlFilter f : sqlFilters) {
            allParams.addAll(f.params());
        }

        String countSql = "SELECT COUNT(*) FROM (" + baseSelectSql + whereClause + ") AS base";
        long total = jdbcTemplate.queryForObject(countSql, allParams.toArray(), Long.class);

        if (pageable == null || pageable.isUnpaged()) {
            String finalSql = "SELECT * FROM (" + baseSelectSql + whereClause + ") AS base ORDER BY base.oid";
            List<UserProfileDto> content = jdbcTemplate.query(
                    finalSql,
                    allParams.toArray(),
                    new BeanPropertyRowMapper<>(UserProfileDto.class)
            );
            return new PageImpl<>(content, Pageable.unpaged(), total);
        }

        String sortClause = pageable.getSort().isEmpty()
                ? " ORDER BY final.oid "
                : " ORDER BY " + pageable.getSort().stream()
                .map(order -> "final." + order.getProperty() + " " + order.getDirection())
                .collect(Collectors.joining(", "));

        String subQuery = "(SELECT * FROM (" + baseSelectSql + whereClause + " LIMIT ? OFFSET ?) AS sub) AS final";
        allParams.add(pageable.getPageSize());
        allParams.add(pageable.getOffset());

        String finalSql = "SELECT * FROM " + subQuery + sortClause;

        List<UserProfileDto> content = jdbcTemplate.query(
                finalSql,
                allParams.toArray(),
                new BeanPropertyRowMapper<>(UserProfileDto.class)
        );

        return new PageImpl<>(content, pageable, total);
    }



}
