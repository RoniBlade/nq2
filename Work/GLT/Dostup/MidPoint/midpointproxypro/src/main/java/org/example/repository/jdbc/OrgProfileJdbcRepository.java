//package org.example.repository.jdbc;
//
//import org.example.model.filter.FilterNode;
//import org.example.repository.JdbcRepository;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.Function;
//
//@Repository
//public class OrgProfileJdbcRepository extends JdbcRepository<Map<String, Object>> {
//
//    public OrgProfileJdbcRepository(JdbcTemplate jdbc) {
//        super(jdbc);
//    }
//
//    private static final String BASE_SELECT = """
//            SELECT
//                m_org.oid::text AS oid,
//                zzz.value::json ->> 'oid'::text AS parentorgoid,
//                zzz.value::json ->> 'type'::text AS parentorgtype,
//                zzz.value::json ->> 'relation'::text AS parentorgrelation,
//                m_org.namenorm,
//                m_org.nameorig,
//                ((convert_from(m_org.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'description'::text AS description,
//                ((convert_from(m_org.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'documentation'::text AS documentation,
//                ((convert_from(m_org.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'indestructible'::text AS indestructible,
//                m_org.fullobject,
//                m_org.tenantreftargetoid,
//                m_org.tenantreftargettype,
//                m_org.tenantrefrelationid,
//                m_org.lifecyclestate,
//                m_org.cidseq,
//                m_org.version,
//                m_org.policysituations,
//                m_org.subtypes,
//                m_org.fulltextinfo,
//                m_org.ext,
//                m_org.creatorreftargetoid,
//                m_org.creatorreftargettype,
//                m_org.creatorrefrelationid,
//                m_org.createchannelid,
//                m_org.createtimestamp,
//                m_org.modifierreftargetoid,
//                m_org.modifierreftargettype,
//                m_org.modifierrefrelationid,
//                m_org.modifychannelid,
//                m_org.modifytimestamp,
//                m_org.db_created AS dbcreated,
//                m_org.db_modified AS dbmodified,
//                m_org.objecttype,
//                m_org.costcenter,
//                m_org.emailaddress,
//                m_org.photo,
//                m_org.locale,
//                m_org.localityorig,
//                m_org.localitynorm,
//                m_org.preferredlanguage,
//                m_org.telephonenumber,
//                m_org.timezone,
//                m_org.passwordcreatetimestamp,
//                m_org.passwordmodifytimestamp,
//                m_org.administrativestatus,
//                m_org.effectivestatus,
//                m_org.enabletimestamp,
//                m_org.disabletimestamp,
//                m_org.disablereason,
//                m_org.validitystatus,
//                m_org.validfrom,
//                m_org.validto,
//                m_org.validitychangetimestamp,
//                m_org.archivetimestamp,
//                m_org.lockoutstatus,
//                m_org.normalizeddata,
//                m_org.autoassignenabled,
//                m_org.displaynameorig,
//                m_org.displaynamenorm,
//                m_org.identifier,
//                m_org.requestable,
//                m_org.risklevel,
//                m_org.displayorder,
//                m_org.tenant,
//                ma.oid AS targetarchetypeoid,
//                ma.nameorig AS targetarchetypenameorig,
//                ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'cssClass'::text AS archetypeicon,
//                ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'color'::text AS archetypecolor,
//                public.glt_get_org_displayname(m_org.oid::text) AS vdisplayname
//               FROM m_org
//                 LEFT JOIN LATERAL jsonb_array_elements(public.glt_texttoarrayjsonb(((convert_from(m_org.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'parentOrgRef'::text)) zzz(value) ON true
//                 LEFT JOIN m_archetype ma ON ((((((convert_from(m_org.fullobject, 'UTF8'::name)::json ->> 'org'::character varying::text)::json) ->> 'archetypeRef'::text)::jsonb) ->> 'oid'::text)::uuid) = ma.oid
//            """;
//
//    // ===== нормализация имён (snake/camel → без подчёркиваний, lower) =====
//    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }
//
//    // Алиас -> выражение ДО AS (для WHERE). Здесь всё простое: колонки из reqq.*
//    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
//            Map.entry("oid", "oid"),
//            Map.entry("parentorgoid", ""),
//            Map.entry("parentorgtype", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            Map.entry("", ""),
//            // id — это window-функция (row_number) и в WHERE не используется
//    );
//
//    // DTO-имена (camelCase) -> алиасы из SELECT
//    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
//            Map.entry("ownerOid", "owneroid"),
//            Map.entry("targetOid", "targetoid")
//    );
//
//    private String resolve(String field) {
//        String n = norm(field);
//        // DTO -> алиас -> expr
//        for (var e : DTO_TO_ALIAS.entrySet()) {
//            if (Objects.equals(norm(e.getKey()), n)) {
//                String alias = e.getValue();
//                String expr = ALIAS_TO_EXPR.get(norm(alias));
//                return expr != null ? expr : alias;
//            }
//        }
//        // алиас -> expr
//        for (var e : ALIAS_TO_EXPR.entrySet()) {
//            if (Objects.equals(norm(e.getKey()), n)) return e.getValue();
//        }
//        // по умолчанию — пробуем как reqq.<имя_без_мусора>
//        String cleaned = field == null ? "" : field.replaceAll("[^A-Za-z0-9_]", "");
//        return "reqq." + cleaned;
//    }
//
//    // ===== реализация абстрактных методов базового репо =====
//    @Override protected String baseSelect() { return BASE_SELECT; }
//
//    @Override protected boolean baseHasWhere() { return true; } // сверху WHERE нет → TinySql вставит сам
//
//    @Override protected Function<String, String> fieldResolver() { return this::resolve; }
//
//    @Override protected Map<String, Object> mapRow(Map<String, Object> row) { return row; }
//
//    // ===== публичные методы =====
//    public Page<Map<String, Object>> findAll(Pageable pageable) {
//        return super.page(Collections.emptyList(), pageable);
//    }
//
//    public Page<Map<String, Object>> findByFilters(List<FilterNode> filters, Pageable pageable) {
//        return super.page(filters, pageable);
//    }
//
//}
