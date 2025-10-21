package org.example.repository.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.ApprovalRoleInfoDto;
import org.example.model.filter.FilterNode;
import org.example.repository.BaseJdbcRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;

@Repository
@Slf4j
public class ApprovalRoleInfoJdbcRepository extends BaseJdbcRepo<ApprovalRoleInfoDto> {

    public ApprovalRoleInfoJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String BASE_SELECT = """
        SELECT
            row_number() OVER () AS id,
            ma.owneroid,
            ma.containertype::text AS containertype,
            ma.targetreftargettype::text AS targettype,
            CASE
                WHEN mo.objecttype = 'ORG'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'org')::json) ->> 'name'
                WHEN mo.objecttype = 'ROLE'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'name'
                WHEN mo.objecttype = 'SERVICE'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'service')::json) ->> 'name'
                ELSE NULL
            END AS objectname,
            CASE
                WHEN mo.objecttype = 'ORG'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'org')::json) ->> 'displayName'
                WHEN mo.objecttype = 'ROLE'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'displayName'
                WHEN mo.objecttype = 'SERVICE'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'service')::json) ->> 'displayName'
                ELSE NULL
            END AS objectdisplayname,
            CASE
                WHEN mo.objecttype = 'ORG'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'org')::json) ->> 'description'
                WHEN mo.objecttype = 'ROLE'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'role')::json) ->> 'description'
                WHEN mo.objecttype = 'SERVICE'::objecttype
                    THEN ((convert_from(mo.fullobject, 'UTF8')::json ->> 'service')::json) ->> 'description'
                ELSE NULL
            END AS objectdescription
        FROM m_assignment ma
        JOIN m_object mo ON ma.targetreftargetoid = mo.oid
        WHERE ma.targetreftargettype = 'ROLE'::objecttype
          AND ma.ownertype           = 'ROLE'::objecttype
        """;

    // ==== Алиасы -> выражения ДО AS ====
    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("owneroid", "ma.owneroid"),
            Map.entry("containertype", "ma.containertype::text"),
            Map.entry("targettype", "ma.targetreftargettype::text"),
            Map.entry("objectname",
                    "CASE WHEN mo.objecttype='ORG'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'org')::json)->>'name' " +
                            "WHEN mo.objecttype='ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'role')::json)->>'name' " +
                            "WHEN mo.objecttype='SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'service')::json)->>'name' ELSE NULL END"),
            Map.entry("objectdisplayname",
                    "CASE WHEN mo.objecttype='ORG'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'org')::json)->>'displayName' " +
                            "WHEN mo.objecttype='ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'role')::json)->>'displayName' " +
                            "WHEN mo.objecttype='SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'service')::json)->>'displayName' ELSE NULL END"),
            Map.entry("objectdescription",
                    "CASE WHEN mo.objecttype='ORG'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'org')::json)->>'description' " +
                            "WHEN mo.objecttype='ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'role')::json)->>'description' " +
                            "WHEN mo.objecttype='SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json->>'service')::json)->>'description' ELSE NULL END")
    );

    private static String norm(String s) { return s == null ? null : s.replace("_", "").toLowerCase(); }

    // ==== DTO поле -> алиас в SELECT ====
    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("ownerOid", "owneroid"),
            Map.entry("containerType", "containertype"),
            Map.entry("targetType", "targettype"),
            Map.entry("objectName", "objectname"),
            Map.entry("objectDisplayName", "objectdisplayname"),
            Map.entry("objectDescription", "objectdescription")
    );

    private String resolve(String field) {
        String n = norm(field);

        // DTO -> алиас -> выражение
        for (var e : DTO_TO_ALIAS.entrySet()) {
            if (Objects.equals(norm(e.getKey()), n)) {
                String alias = e.getValue();
                String expr = ALIAS_TO_EXPR.get(norm(alias));
                return expr != null ? expr : alias;
            }
        }
        // алиас -> выражение
        for (var e : ALIAS_TO_EXPR.entrySet()) {
            if (Objects.equals(norm(e.getKey()), n)) return e.getValue();
        }
        // по умолчанию — как есть
        return field;
    }

    @Override protected String baseSelect() { return BASE_SELECT; }

    @Override protected boolean baseHasWhere() { return true; }

    @Override protected Function<String, String> fieldResolver() { return this::resolve; }

    @Override
    protected ApprovalRoleInfoDto mapRow(Map<String, Object> row) {
        ApprovalRoleInfoDto dto = new ApprovalRoleInfoDto();
        Object id = row.get("id");
        if (id instanceof Number num) dto.setId(num.longValue());

        Object owner = row.get("owneroid");
        if (owner instanceof java.util.UUID u) {
            dto.setOwneroid(u);
        } else if (owner instanceof String s) {
            try { dto.setOwneroid(java.util.UUID.fromString(s)); } catch (Exception ignored) {}
        }

        dto.setContainerType((String) row.get("containertype"));
        dto.setTargetType((String) row.get("targettype"));
        dto.setObjectName((String) row.get("objectname"));
        dto.setObjectDisplayName((String) row.get("objectdisplayname"));
        dto.setObjectDescription((String) row.get("objectdescription"));
        return dto;
    }

    // Удобные методы
    public Page<ApprovalRoleInfoDto> findAll(Pageable pageable) {
        return super.page(Collections.emptyList(), pageable);
    }

    public Page<ApprovalRoleInfoDto> findByFilters(List<FilterNode> filters, Pageable pageable) {
        return super.page(filters, pageable);
    }
}
