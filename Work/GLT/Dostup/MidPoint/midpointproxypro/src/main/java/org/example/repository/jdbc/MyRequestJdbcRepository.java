package org.example.repository.jdbc;

import lombok.extern.slf4j.Slf4j;
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
public class MyRequestJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {

    public MyRequestJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String BASE_SELECT = """
        SELECT row_number() OVER () AS id,
               reqq.caseoid,
               reqq.casedatecreate,
               reqq.objecttype,
               reqq.objectid,
               reqq.objectname,
               reqq.objectdisplayname,
               reqq.objectdescription,
               reqq.requestername,
               reqq.requesterfullname,
               reqq.requestertitle,
               reqq.asrequesterorganization,
               reqq.requesteremail,
               reqq.requesterphone,
               reqq.closetime,
               reqq.state,
               reqq.targetoid,
               reqq.targetnamenorm
        FROM (
            SELECT mc.oid AS caseoid,
                   mc.db_created AS casedatecreate,
                   mo.objecttype::text AS objecttype,
                   mo.oid::text AS objectid,
                   CASE
                     WHEN mo.objecttype = 'ORG'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'org')::json)->>'name'
                     WHEN mo.objecttype = 'ROLE'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'role')::json)->>'name'
                     ELSE NULL::text
                   END AS objectname,
                   CASE
                     WHEN mo.objecttype = 'ORG'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'org')::json)->>'displayName'
                     WHEN mo.objecttype = 'ROLE'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'role')::json)->>'displayName'
                     ELSE NULL::text
                   END AS objectdisplayname,
                   CASE
                     WHEN mo.objecttype = 'ORG'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'org')::json)->>'description'
                     WHEN mo.objecttype = 'ROLE'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'role')::json)->>'description'
                     ELSE NULL::text
                   END AS objectdescription,
                   mu1.nameorig AS requestername,
                   mu1.fullnameorig AS requesterfullname,
                   mu1.titleorig AS requestertitle,
                   (mu1.organizations -> 0) ->> 'o'::text AS asrequesterorganization,
                   mu1.emailaddress AS requesteremail,
                   mu1.telephonenumber AS requesterphone,
                   mc.closetimestamp AS closetime,
                   mc.state,
                   mu.oid AS targetoid,
                   mu.namenorm AS targetnamenorm
            FROM m_case mc, m_user mu, m_user mu1, m_object mo
            WHERE mc.targetreftargetoid = mo.oid
              AND mc.parentreftargetoid IS NOT NULL
              AND mc.objectreftargetoid = mu.oid
              AND mc.requestorreftargetoid = mu1.oid
            UNION ALL
            SELECT mc.oid AS caseoid,
                   mc.db_created AS casedatecreate,
                   mo.objecttype::text AS objecttype,
                   mo.oid::text AS objectid,
                   CASE
                     WHEN mo.objecttype = 'ORG'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'org')::json)->>'name'
                     WHEN mo.objecttype = 'ROLE'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'role')::json)->>'name'
                     ELSE NULL::text
                   END AS objectname,
                   CASE
                     WHEN mo.objecttype = 'ORG'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'org')::json)->>'displayName'
                     WHEN mo.objecttype = 'ROLE'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'role')::json)->>'displayName'
                     ELSE NULL::text
                   END AS objectdisplayname,
                   CASE
                     WHEN mo.objecttype = 'ORG'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'org')::json)->>'description'
                     WHEN mo.objecttype = 'ROLE'::objecttype
                       THEN ((convert_from(mo.fullobject,'UTF8'::name)::json ->> 'role')::json)->>'description'
                     ELSE NULL::text
                   END AS objectdescription,
                   mu1.nameorig AS requestername,
                   mu1.fullnameorig AS requesterfullname,
                   mu1.titleorig AS requestertitle,
                   (mu1.organizations -> 0) ->> 'o'::text AS asrequesterorganization,
                   mu1.emailaddress AS requesteremail,
                   mu1.telephonenumber AS requesterphone,
                   mc.closetimestamp AS closetime,
                   mc.state,
                   mu1.oid AS targetoid,
                   mu.namenorm AS targetnamenorm
            FROM m_case mc, m_user mu, m_user mu1, m_object mo
            WHERE mc.targetreftargetoid = mo.oid
              AND mc.parentreftargetoid IS NOT NULL
              AND mc.objectreftargetoid = mu.oid
              AND mc.requestorreftargetoid = mu1.oid
              AND (mu.oid IN (SELECT dd.targetoid FROM m_ref_persona dd))
        ) reqq
        """;

    // ===== нормализация имён (snake/camel → без подчёркиваний, lower) =====
    private static String norm(String s) { return s == null ? null : s.replace("_", "").toLowerCase(); }

    // Алиас -> выражение ДО AS (для WHERE). Здесь всё простое: колонки из reqq.*
    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("caseoid", "reqq.caseoid"),
            Map.entry("casedatecreate", "reqq.casedatecreate"),
            Map.entry("objecttype", "reqq.objecttype"),
            Map.entry("objectid", "reqq.objectid"),
            Map.entry("objectname", "reqq.objectname"),
            Map.entry("objectdisplayname", "reqq.objectdisplayname"),
            Map.entry("objectdescription", "reqq.objectdescription"),
            Map.entry("requestername", "reqq.requestername"),
            Map.entry("requesterfullname", "reqq.requesterfullname"),
            Map.entry("requestertitle", "reqq.requestertitle"),
            Map.entry("asrequesterorganization", "reqq.asrequesterorganization"),
            Map.entry("requesteremail", "reqq.requesteremail"),
            Map.entry("requesterphone", "reqq.requesterphone"),
            Map.entry("closetime", "reqq.closetime"),
            Map.entry("state", "reqq.state"),
            Map.entry("targetoid", "reqq.targetoid"),
            Map.entry("targetnamenorm", "reqq.targetnamenorm")
            // id — это window-функция (row_number) и в WHERE не используется
    );

    // DTO-имена (camelCase) -> алиасы из SELECT
    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("caseOid", "caseoid"),
            Map.entry("caseDateCreate", "casedatecreate"),
            Map.entry("objectType", "objecttype"),
            Map.entry("objectId", "objectid"),
            Map.entry("objectName", "objectname"),
            Map.entry("objectDisplayName", "objectdisplayname"),
            Map.entry("objectDescription", "objectdescription"),
            Map.entry("requesterName", "requestername"),
            Map.entry("requesterFullName", "requesterfullname"),
            Map.entry("requesterTitle", "requestertitle"),
            Map.entry("asRequesterOrganization", "asrequesterorganization"),
            Map.entry("requesterEmail", "requesteremail"),
            Map.entry("requesterPhone", "requesterphone"),
            Map.entry("closeTime", "closetime"),
            Map.entry("state", "state"),
            Map.entry("targetOid", "targetoid"),
            Map.entry("targetNameNorm", "targetnamenorm")
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

    @Override protected boolean baseHasWhere() { return false; } // сверху WHERE нет → TinySql вставит сам

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
