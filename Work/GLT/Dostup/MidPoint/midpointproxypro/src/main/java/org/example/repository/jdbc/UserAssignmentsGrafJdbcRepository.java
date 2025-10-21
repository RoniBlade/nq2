package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.UserAssignmentsGrafDto;
import org.example.model.filter.FilterNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserAssignmentsGrafJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public Page<UserAssignmentsGrafDto> findAll(Pageable pageable, List<FilterNode> filters) {
        Object ownerOid = extractOwnerOid(filters);
        if (ownerOid == null) {
            throw new IllegalArgumentException("❌ Не найден фильтр по 'ownerOid'");
        }

        String baseSql = """
            SELECT * FROM (
                SELECT 'INDIRECT' AS tab,
                       false AS isRequest,
                       mrrm.owneroid,
                       mrrm.targetoid,
                       mrrm.targettype::text AS targetType,
                       tgt.nameorig AS targetNameOrig,
                       glt_parsepolystring(((convert_from(tgt.fullobject, 'UTF8')::json ->> lower(tgt.objecttype::text))::json) ->> 'displayName') AS targetDisplayName,
                       ma.nameorig AS targetArchetypeNameOrig,
                       ((((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json) ->> 'cssClass' AS archetypeIconClass,
                       ((((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json) ->> 'color' AS archetypeIconColor
                FROM m_ref_role_membership mrrm
                         JOIN m_object tgt ON mrrm.targetoid = tgt.oid
                         LEFT JOIN m_assignment mass ON mrrm.owneroid = mass.owneroid AND mrrm.targetoid = mass.targetreftargetoid
                         LEFT JOIN m_archetype ma ON ((((((convert_from(tgt.fullobject, 'UTF8')::json ->> lower(tgt.objecttype::text))::json) ->> 'archetypeRef')::json) ->> 'oid')::uuid) = ma.oid
                WHERE mrrm.ownertype = 'USER'::objecttype
                  AND mrrm.targettype::text = ANY (ARRAY['ROLE', 'ORG', 'USER', 'CASE'])
                  AND mrrm.owneroid = ?::uuid

                UNION ALL

                SELECT '' AS tab,
                       false AS isRequest,
                       mrp.owneroid,
                       ms.oid AS targetOid,
                       'ACCOUNT' AS targetType,
                       mr.nameorig AS targetNameOrig,
                       mr.namenorm AS targetDisplayName,
                       '' AS targetArchetypeNameOrig,
                       '' AS archetypeIconClass,
                       '' AS archetypeIconColor
                FROM m_ref_projection mrp
                         JOIN m_shadow ms ON ms.oid = mrp.targetoid
                         JOIN m_resource mr ON ms.resourcereftargetoid = mr.oid
                WHERE mrp.ownertype::text = ANY (ARRAY['USER', 'ROLE', 'ORG'])
                  AND mrp.owneroid = ?::uuid

                UNION ALL

                SELECT DISTINCT '' AS tab,
                                false AS isRequest,
                                mrp.owneroid,
                                mrp.targetoid,
                                'PERSONAS' AS targetType,
                                per.nameorig AS targetNameOrig,
                                glt_get_user_displayname(per.oid::text) AS targetDisplayName,
                                ma.nameorig AS targetArchetypeNameOrig,
                                ((((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json) ->> 'cssClass' AS archetypeIconClass,
                                ((((((((convert_from(ma.fullobject, 'UTF8')::json ->> 'archetype')::json) ->> 'archetypePolicy')::json) ->> 'display')::json) ->> 'icon')::json) ->> 'color' AS archetypeIconColor
                FROM m_ref_persona mrp
                         JOIN m_user own ON mrp.owneroid = own.oid
                         JOIN m_user per ON mrp.targetoid = per.oid
                         LEFT JOIN m_archetype ma ON ((((((convert_from(per.fullobject, 'UTF8')::json ->> 'user')::json) ->> 'archetypeRef')::jsonb) ->> 'oid')::uuid) = ma.oid
                WHERE mrp.owneroid = ?::uuid
            ) AS base
            WHERE true
            LIMIT ? OFFSET ?
        """;

        String countSql = """
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM m_ref_role_membership mrrm
                WHERE mrrm.ownertype = 'USER'::objecttype
                  AND mrrm.targettype::text = ANY (ARRAY['ROLE', 'ORG', 'USER', 'CASE'])
                  AND mrrm.owneroid = ?::uuid
                UNION
                SELECT 1
                FROM m_ref_projection mrp
                WHERE mrp.ownertype::text = ANY (ARRAY['USER', 'ROLE', 'ORG'])
                  AND mrp.owneroid = ?::uuid
                UNION
                SELECT 1
                FROM m_ref_persona mrp
                WHERE mrp.owneroid = ?::uuid
            ) AS count_query
        """;

        List<Object> countParams = List.of(ownerOid, ownerOid, ownerOid);
        Long total = jdbcTemplate.queryForObject(countSql, countParams.toArray(), Long.class);

        List<Object> dataParams = new ArrayList<>();
        dataParams.add(ownerOid);
        dataParams.add(ownerOid);
        dataParams.add(ownerOid);
        dataParams.add(pageable.getPageSize());
        dataParams.add((long) pageable.getOffset());

        List<UserAssignmentsGrafDto> content = jdbcTemplate.query(
                baseSql,
                dataParams.toArray(),
                new BeanPropertyRowMapper<>(UserAssignmentsGrafDto.class)
        );

        return new PageImpl<>(content, pageable, total);
    }

    private Object extractOwnerOid(List<FilterNode> nodes) {
        if (nodes == null) return null;

        for (FilterNode node : nodes) {
            if (node == null) continue;

            if (node.isLeaf()) {
                var criterion = node.getCriterion();
                if (criterion != null && "ownerOid".equalsIgnoreCase(criterion.getField())) {
                    return criterion.getValue();
                }
            } else if (node.getFilters() != null) {
                Object result = extractOwnerOid(node.getFilters());
                if (result != null) return result;
            }
        }
        return null;
    }
}
