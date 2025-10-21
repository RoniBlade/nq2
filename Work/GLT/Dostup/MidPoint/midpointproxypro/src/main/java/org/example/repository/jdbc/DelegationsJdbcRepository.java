package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
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
public class DelegationsJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {

    private static final String BASE_SELECT = """
         WITH assignments AS (
                 SELECT mass_1.owneroid,
                    mass_1.cid,
                    mass_1.containertype,
                    mass_1.ownertype,
                    mass_1.lifecyclestate,
                    mass_1.ordervalue,
                    mass_1.orgreftargetoid,
                    mass_1.orgreftargettype,
                    mass_1.orgrefrelationid,
                    mass_1.targetreftargetoid,
                    mass_1.targetreftargettype,
                    mass_1.targetrefrelationid,
                    mass_1.tenantreftargetoid,
                    mass_1.tenantreftargettype,
                    mass_1.tenantrefrelationid,
                    mass_1.policysituations,
                    mass_1.subtypes,
                    mass_1.ext,
                    mass_1.resourcereftargetoid,
                    mass_1.resourcereftargettype,
                    mass_1.resourcerefrelationid,
                    mass_1.administrativestatus,
                    mass_1.effectivestatus,
                    mass_1.enabletimestamp,
                    mass_1.disabletimestamp,
                    mass_1.disablereason,
                    mass_1.validitystatus,
                    mass_1.validfrom,
                    mass_1.validto,
                    mass_1.validitychangetimestamp,
                    mass_1.archivetimestamp,
                    mass_1.creatorreftargetoid,
                    mass_1.creatorreftargettype,
                    mass_1.creatorrefrelationid,
                    mass_1.createchannelid,
                    mass_1.createtimestamp,
                    mass_1.modifierreftargetoid,
                    mass_1.modifierreftargettype,
                    mass_1.modifierrefrelationid,
                    mass_1.modifychannelid,
                    mass_1.modifytimestamp,
                    mass_1.fullobject,
                    zzz.value
                   FROM m_assignment mass_1
                     LEFT JOIN LATERAL jsonb_array_elements(public.glt_texttoarrayjsonb(((((convert_from(mass_1.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitTargetContent'::text)::json) ->> 'targetRef'::text)) zzz(value) ON true
                  WHERE mass_1.ownertype = 'USER'::objecttype AND mass_1.targetreftargettype = 'USER'::objecttype AND (((((convert_from(mass_1.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'targetRef'::text)::json) ->> 'relation'::text) = 'org:deputy'::text
                )
         SELECT deputy.oid::character varying AS dep_oid,
            deputy.fullnameorig AS dep_fullname,
            deputy.personalnumber AS dep_personalnumber,
            deputy.emailaddress AS dep_emailaddress,
            delegator.oid::character varying AS del_oid,
            delegator.fullnameorig AS del_fullname,
            delegator.personalnumber AS del_personalnumber,
            delegator.emailaddress AS del_emailaddress,
            ((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'description'::text AS description,
            ((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'activation'::text)::json) ->> 'effectiveStatus'::text AS effectivestatus,
            to_timestamp(((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'activation'::text)::json) ->> 'validFrom'::text, 'YYYY-MM-DD"T"HH24:MI:SS.USO'::text) AS validfrom,
            to_timestamp(((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'activation'::text)::json) ->> 'validTo'::text, 'YYYY-MM-DD"T"HH24:MI:SS.USO'::text) AS validto,
                CASE
                    WHEN (((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitTargetContent'::text)::json) ->> 'allowTransitive'::text) = 'true'::text THEN true
                    WHEN (((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitTargetContent'::text)::json) ->> 'allowTransitive'::text) = 'false'::text THEN false
                    ELSE false
                END AS allowtransitive,
                CASE
                    WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'certificationWorkItems'::text)::json) ->> 'all'::text) = 'true'::text THEN true
                    WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'certificationWorkItems'::text)::json) ->> 'all'::text) = 'false'::text THEN false
                    ELSE false
                END AS certificationworkitems,
                CASE
                    WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'caseManagementWorkItems'::text)::json) ->> 'all'::text) = 'true'::text THEN true
                    WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'caseManagementWorkItems'::text)::json) ->> 'all'::text) = 'false'::text THEN false
                    ELSE false
                END AS casemanagementworkitems,
            mass.value ->> 'oid'::text AS delegated_item_oid,
                CASE
                    WHEN (mass.value ->> 'type'::text) = 'c:RoleType'::text THEN 'ROLE'::text
                    WHEN (mass.value ->> 'type'::text) = 'c:OrgType'::text THEN 'ORG'::text
                    ELSE mass.value ->> 'type'::text
                END AS delegated_item_type,
            mass.value ->> 'relation'::text AS delegated_item_relation,
            di.nameorig AS delegated_item_nameorig,
            di.objecttype::text AS delegated_item_objecttype,
            ((convert_from(di.fullobject, 'UTF8'::name)::json ->> lower(di.objecttype::text))::json) ->> 'displayName'::text AS delegated_item_displayname,
            ((convert_from(di.fullobject, 'UTF8'::name)::json ->> lower(di.objecttype::text))::json) ->> 'description'::text AS delegated_item_description,
            ma.nameorig AS delegated_item_archetype_name,
            ((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'displayName'::text AS delegated_item_archetype_displayname,
            ((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'pluralLabel'::text AS delegated_item_archetype_plural,
            ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'cssClass'::text AS delegated_item_archetype_icon_class,
            ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'color'::text AS delegated_item_archetype_icon_color
           FROM assignments mass
             JOIN m_user delegator ON delegator.oid = mass.targetreftargetoid
             JOIN m_user deputy ON deputy.oid = mass.owneroid
             LEFT JOIN m_object di ON di.oid = ((mass.value ->> 'oid'::text)::uuid)
             LEFT JOIN m_archetype ma ON ((((((convert_from(di.fullobject, 'UTF8'::name)::json ->> lower(di.objecttype::character varying::text))::json) ->> 'archetypeRef'::text)::json) ->> 'oid'::text)::uuid) = ma.oid
          WHERE mass.ownertype = 'USER'::objecttype AND mass.targetreftargettype = 'USER'::objecttype AND (((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'targetRef'::text)::json) ->> 'relation'::text) = 'org:deputy'::text
""";

    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("dep_oid", "dep_oid"),
            Map.entry("dep_fulName", "dep_fullname"),
            Map.entry("dep_personalNumber", ""),
            Map.entry("dep_emailAddress", ""),
            Map.entry("del_oid", ""),
            Map.entry("del_fullName", ""),
            Map.entry("del_personalNumber", ""),
            Map.entry("del_emailAddress", "del_emailaddress"),
            Map.entry("description", "description"),
            Map.entry("effectiveStatus", "effectivestatus"),
            Map.entry("validFrom", "validfrom"),
            Map.entry("validTo", "validto"),
            Map.entry("allowTransitive", "allowtransitive"),
            Map.entry("certificationWorkItems", "certificationworkitems"),
            Map.entry("caseManagementWorkItems", "casemanagementworkitems"),
            Map.entry("delegated_item_oid", "delegated_item_oid"),
            Map.entry("delegated_item_type", "delegated_item_type"),
            Map.entry("delegated_item_relation", "delegated_item_relation"),
            Map.entry("delegated_item_nameOrig", "delegated_item_nameorig"),
            Map.entry("delegated_item_objectType", "delegated_item_objecttype"),
            Map.entry("delegated_item_displayName", "delegated_item_displayname"),
            Map.entry("delegated_item_description", "delegated_item_description"),
            Map.entry("delegated_item_archetype_name", "delegated_item_archetype_name"),
            Map.entry("delegated_item_archetype_displayName", "delegated_item_archetype_displayname"),
            Map.entry("delegated_item_archetype_plural", "delegated_item_archetype_plural"),
            Map.entry("delegated_item_archetype_icon_class", "delegated_item_archetype_icon_class"),
            Map.entry("delegated_item_archetype_icon_color", "delegated_item_archetype_icon_color")
    );

    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("dep_oid", "deputy.oid::character varying"),
            Map.entry("dep_fullname", "deputy.fullnameorig"),
            Map.entry("dep_personalnumber", "deputy.personalnumber"),
            Map.entry("dep_emailaddress", "deputy.emailaddress"),
            Map.entry("del_oid", "delegator.oid::character"),
            Map.entry("del_fullname", "delegator.fullnameorig"),
            Map.entry("del_personalnumber", "delegator.personalnumber"),
            Map.entry("del_emailaddress", "delegator.emailaddress"),
            Map.entry("description", "((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'description'::text"),
            Map.entry("effectivestatus", "((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'activation'::text)::json) ->> 'effectiveStatus'::text"),
            Map.entry("validfrom", "to_timestamp(((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'activation'::text)::json) ->> 'validFrom'::text, 'YYYY-MM-DD\"T\"HH24:MI:SS.USO'::text)"),
            Map.entry("validto", "    to_timestamp(((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'activation'::text)::json) ->> 'validTo'::text, 'YYYY-MM-DD\"T\"HH24:MI:SS.USO'::text)"),
            Map.entry("allowtransitive", "        CASE\n"
                    + "            WHEN (((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitTargetContent'::text)::json) ->> 'allowTransitive'::text) = 'true'::text THEN true\n"
                    + "            WHEN (((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitTargetContent'::text)::json) ->> 'allowTransitive'::text) = 'false'::text THEN false\n"
                    + "            ELSE false\n"
                    + "        END"),
            Map.entry("certificationworkitems", "        CASE\n"
                    + "            WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'certificationWorkItems'::text)::json) ->> 'all'::text) = 'true'::text THEN true\n"
                    + "            WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'certificationWorkItems'::text)::json) ->> 'all'::text) = 'false'::text THEN false\n"
                    + "            ELSE false\n"
                    + "        END"),
            Map.entry("casemanagementworkitems", "        CASE\n"
                    + "            WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'caseManagementWorkItems'::text)::json) ->> 'all'::text) = 'true'::text THEN true\n"
                    + "            WHEN (((((((convert_from(mass.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'limitOtherPrivileges'::text)::json) ->> 'caseManagementWorkItems'::text)::json) ->> 'all'::text) = 'false'::text THEN false\n"
                    + "            ELSE false\n"
                    + "        END"),
            Map.entry("delegated_item_oid", "mass.value ->> 'oid'::text"),
            Map.entry("delegated_item_type", "        CASE\n"
                    + "            WHEN (mass.value ->> 'type'::text) = 'c:RoleType'::text THEN 'ROLE'::text\n"
                    + "            WHEN (mass.value ->> 'type'::text) = 'c:OrgType'::text THEN 'ORG'::text\n"
                    + "            ELSE mass.value ->> 'type'::text\n"
                    + "        END"),
            Map.entry("delegated_item_relation", "mass.value ->> 'relation'::text"),
            Map.entry("delegated_item_nameorig", "di.nameorig"),
            Map.entry("delegated_item_objecttype", "di.objecttype::text"),
            Map.entry("delegated_item_displayname", "((convert_from(di.fullobject, 'UTF8'::name)::json ->> lower(di.objecttype::text))::json) ->> 'displayName'::text"),
            Map.entry("delegated_item_description", "((convert_from(di.fullobject, 'UTF8'::name)::json ->> lower(di.objecttype::text))::json) ->> 'description'::text"),
            Map.entry("delegated_item_archetype_name", "ma.nameorig"),
            Map.entry("delegated_item_archetype_displayname", "((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'displayName'::text"),
            Map.entry("delegated_item_archetype_plural", "((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'pluralLabel'::text"),
            Map.entry("delegated_item_archetype_icon_class", "((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'cssClass'::text"),
            Map.entry("delegated_item_archetype_icon_color", "((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'color'::text")
    );

    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

    public DelegationsJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

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
