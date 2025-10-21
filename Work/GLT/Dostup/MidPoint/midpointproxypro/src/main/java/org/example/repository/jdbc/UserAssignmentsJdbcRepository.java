package org.example.repository.jdbc;

import org.example.model.filter.FilterNode;
import org.example.repository.BaseJdbcRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Repository
public class UserAssignmentsJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {

    public UserAssignmentsJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String BASE_SELECT = """
           SELECT
                 CASE
                     WHEN ah.predecessor_oid IS NULL OR ah.predecessor_order = 1 AND ah.predecessor_oid = mrrm.targetoid::text THEN 'DIRECT'::text
                     ELSE 'INDIRECT'::text
                 END AS tab,
                 CASE
                     WHEN NOT (ah.predecessor_oid IS NULL OR ah.predecessor_order = 1 AND ah.predecessor_oid = mrrm.targetoid::text) THEN false
                     WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> '@metadata'::text)::json) ->> 'process'::text)::json) ->> 'createApproverRef'::text) IS NOT NULL THEN true
                     ELSE false
                 END AS isrequest,
             ((((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> '@metadata'::text)::json) ->> 'process'::text)::json) ->> 'requestorRef'::text)::json) ->> 'oid'::text AS requestor_oid,
             req.nameorig AS requestor_name,
             req.fullnameorig AS requestor_displayname,
             req.personalnumber AS requestor_personalnumber,
             mrrm.owneroid,
             mrrm.ownertype,
             own.nameorig AS owner_nameorig,
                 CASE
                     WHEN own.objecttype = 'USER'::objecttype THEN ((convert_from(own.fullobject, 'UTF8'::name)::json ->> 'user'::text)::json) ->> 'fullName'::text
                     ELSE ((convert_from(own.fullobject, 'UTF8'::name)::json ->> lower(own.objecttype::text))::json) ->> 'displayName'::text
                 END AS owner_displayname,
             mrrm.targetoid,
             mrrm.targettype::text AS targettype,
             tgt.nameorig AS target_nameorig,
             ah.path_number,
             ah.predecessor_oid,
             ah.predecessor_name,
             ah.predecessor_displayname,
             ah.predecessor_objecttype,
             ah.predecessor_order,
             ah.predecessor_archetype_name,
             ah.predecessor_archetype_displayname,
             ah.predecessor_archetype_plural,
             ah.predecessor_archetype_icon_class,
             ah.predecessor_archetype_icon_color,
             ((convert_from(tgt.fullobject, 'UTF8'::name)::json ->> lower(tgt.objecttype::character varying::text))::json) ->> 'displayName'::text AS target_displayname,
             ma.oid AS target_archetype_oid,
             ma.nameorig AS target_archetype_nameorig,
             NULL::timestamp with time zone AS assgn_validfrom,
             NULL::timestamp with time zone AS assgn_validto,
             NULL::activationstatustype AS assgn_administrativestatus,
             NULL::activationstatustype AS assgn_effectivestatus,
             ((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'displayName'::text AS archetype_displayname,
             ((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'pluralLabel'::text AS archetype_plural,
             ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'cssClass'::text AS archetype_icon_class,
             ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'color'::text AS archetype_icon_color
            FROM m_ref_role_membership mrrm
              JOIN m_object tgt ON mrrm.targetoid = tgt.oid
              JOIN m_object own ON mrrm.owneroid = own.oid
              LEFT JOIN m_assignment mass ON mrrm.owneroid = mass.owneroid AND mrrm.targetoid = mass.targetreftargetoid
              LEFT JOIN m_user req ON ((((((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> '@metadata'::text)::json) ->> 'process'::text)::json) ->> 'requestorRef'::text)::json) ->> 'oid'::text)::uuid) = req.oid
              LEFT JOIN m_archetype ma ON ((((((convert_from(tgt.fullobject, 'UTF8'::name)::json ->> lower(tgt.objecttype::character varying::text))::json) ->> 'archetypeRef'::text)::json) ->> 'oid'::text)::uuid) = ma.oid
              LEFT JOIN glt_assignments_path_helper_v ah ON mrrm.owneroid = ah.own_oid AND mrrm.targetoid = ah.tgt_oid
           """;

    // ===== нормализация имён (snake/camel → без подчёркиваний, lower) =====
    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

    // Алиас -> выражение ДО AS (для WHERE). Здесь всё простое: колонки из reqq.*
    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("tab", "CASE\n"
                    + "                     WHEN ah.predecessor_oid IS NULL OR ah.predecessor_order = 1 AND ah.predecessor_oid = mrrm.targetoid::text THEN 'DIRECT'::text\n"
                    + "                     ELSE 'INDIRECT'::text\n"
                    + "                 END"),
            Map.entry("isrequest", "CASE\n"
                    + "                     WHEN NOT (ah.predecessor_oid IS NULL OR ah.predecessor_order = 1 AND ah.predecessor_oid = mrrm.targetoid::text) THEN false\n"
                    + "                     WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> '@metadata'::text)::json) ->> 'process'::text)::json) ->> 'createApproverRef'::text) IS NOT NULL THEN true\n"
                    + "                     ELSE false\n"
                    + "                 END"),
            Map.entry("requestor_oid", "((((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> '@metadata'::text)::json) ->> 'process'::text)::json) ->> 'requestorRef'::text)::json) ->> 'oid'::text"),
            Map.entry("requestor_displayname", "req.fullnameorig"),
            Map.entry("requestor_personalnumber", "req.personalnumber"),
            Map.entry("owneroid", "mrrm.owneroid"),
            Map.entry("ownertype", "mrrm.ownertype"),
            Map.entry("owner_nameorig", "own.nameorig"),
            Map.entry("owner_displayname", "CASE\n"
                    + "            WHEN own.objecttype = 'USER'::objecttype THEN ((convert_from(own.fullobject, 'UTF8'::name)::json ->> 'user'::text)::json) ->> 'fullName'::text\n"
                    + "            ELSE ((convert_from(own.fullobject, 'UTF8'::name)::json ->> lower(own.objecttype::text))::json) ->> 'displayName'::text\n"
                    + "        END"),
            Map.entry("targetoid", "mrrm.targetoid"),
            Map.entry("targettype", "mrrm.targettype::text"),
            Map.entry("target_nameorig", "tgt.nameorig"),
            Map.entry("path_number", "ah.path_number"),
            Map.entry("predecessor_oid", "ah.predecessor_oid"),
            Map.entry("predecessor_name", "ah.predecessor_name"),
            Map.entry("predecessor_displayname", "ah.predecessor_displayname"),
            Map.entry("predecessor_objecttype", "ah.predecessor_objecttype"),
            Map.entry("predecessor_order", "ah.predecessor_order"),
            Map.entry("predecessor_archetype_name", "ah.predecessor_archetype_name"),
            Map.entry("predecessor_archetype_displayname", "ah.predecessor_archetype_displayname"),
            Map.entry("predecessor_archetype_plural", "ah.predecessor_archetype_plural"),
            Map.entry("predecessor_archetype_icon_class", "ah.predecessor_archetype_icon_class"),
            Map.entry("predecessor_archetype_icon_color", "ah.predecessor_archetype_icon_color"),
            Map.entry("target_displayname", "((convert_from(tgt.fullobject, 'UTF8'::name)::json ->> lower(tgt.objecttype::character varying::text))::json) ->> 'displayName'::text"),
            Map.entry("target_archetype_oid", "ma.oid"),
            Map.entry("target_archetype_nameorig", "ma.nameorig"),
            Map.entry("assgn_validfrom", "NULL::timestamp with time zone"),
            Map.entry("assgn_validto", "NULL::timestamp with time zone"),
            Map.entry("assgn_administrativestatus", "NULL::activationstatustype"),
            Map.entry("assgn_effectivestatus", "NULL::activationstatustype"),
            Map.entry("archetype_displayname", "((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'displayName'::text"),
            Map.entry("archetype_plural", "((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'pluralLabel'::text"),
            Map.entry("archetype_icon_class", "((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'cssClass'::text"),
            Map.entry("archetype_icon_color", "((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'color'::text")
    );

    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("tab", "tab"),
            Map.entry("isRequest", "isrequest"),
            Map.entry("requestor_oid", "requestor_oid"),
            Map.entry("requestor_displayName", "requestor_displayname"),
            Map.entry("requestor_personalNumber", "requestor_personalnumber"),
            Map.entry("ownerOid", "owneroid"),
            Map.entry("ownerType", "ownertype"),
            Map.entry("owner_nameOrig", "owner_nameorig"),
            Map.entry("owner_displayName", "owner_displayname"),
            Map.entry("targetOid", "targetoid"),
            Map.entry("targetType", "targettype"),
            Map.entry("target_nameOrig", "target_nameorig"),
            Map.entry("path_number", "path_number"),
            Map.entry("predecessor_oid", "predecessor_oid"),
            Map.entry("predecessor_name", "predecessor_name"),
            Map.entry("predecessor_displayName", "predecessor_displayname"),
            Map.entry("predecessor_objectType", "predecessor_objecttype"),
            Map.entry("predecessor_order", "predecessor_order"),
            Map.entry("predecessor_archetype_name", "predecessor_archetype_name"),
            Map.entry("predecessor_archetype_displayName", "predecessor_archetype_displayname"),
            Map.entry("predecessor_archetype_plural", "predecessor_archetype_plural"),
            Map.entry("predecessor_archetype_icon_class", "predecessor_archetype_icon_class"),
            Map.entry("predecessor_archetype_icon_color", "predecessor_archetype_icon_color"),
            Map.entry("target_displayName", "target_displayname"),
            Map.entry("target_archetype_oid", "target_archetype_oid"),
            Map.entry("target_archetype_nameOrig", "target_archetype_nameorig"),
            Map.entry("assgn_validFrom", "assgn_validfrom"),
            Map.entry("assgn_validTo", "assgn_validto"),
            Map.entry("assgn_administrativeStatus", "assgn_administrativestatus"),
            Map.entry("assgn_effectiveStatus", "assgn_effectivestatus"),
            Map.entry("archetype_displayName", "archetype_displayname"),
            Map.entry("archetype_plural", "archetype_plural"),
            Map.entry("archetype_icon_class", "archetype_icon_class"),
            Map.entry("archetype_icon_color", "archetype_icon_color")
    );

    private String resolve(String field) {
        String n = norm(field);
        // DTO -> алиас -> expr
        for (var e : DTO_TO_ALIAS.entrySet()) {
            if (Objects.equals(norm(e.getKey()), n)) {
                String alias = e.getValue();
                String expr = ALIAS_TO_EXPR.get(norm(alias));
                return expr != null ? expr : alias;
            }
        }
        // алиас -> expr
        for (var e : ALIAS_TO_EXPR.entrySet()) {
            if (Objects.equals(norm(e.getKey()), n)) return e.getValue();
        }
        // по умолчанию — пробуем как reqq.<имя_без_мусора>
        String cleaned = field == null ? "" : field.replaceAll("[^A-Za-z0-9_]", "");
        return "reqq." + cleaned;
    }

    // ===== реализация абстрактных методов базового репо =====
    @Override protected String baseSelect() { return BASE_SELECT; }

    @Override protected boolean baseHasWhere() { return true; } // сверху WHERE нет → TinySql вставит сам

    @Override protected Function<String, String> fieldResolver() { return this::resolve; }

    @Override protected Map<String, Object> mapRow(Map<String, Object> row) { return row; }

    // ===== публичные методы =====
    public Page<Map<String, Object>> findAll(Pageable pageable) {
        return super.page(Collections.emptyList(), pageable);
    }

    public Page<Map<String, Object>> findByFilters(List<FilterNode> filters, Pageable pageable) {
        return super.page(filters, pageable);
    }
}
