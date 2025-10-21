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
public class AccessCertActiveRequestJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {

    private static final String BASE_SELECT = """
           SELECT
            subquery.campaignoid,
            subquery.caseoid,
            subquery.workitemid,
            subquery.objectid,
            subquery.objecttype,
            subquery.objectname,
            subquery.objectdisplayname,
            subquery.objectdescription,
            subquery.objecttitle,
            subquery.objectorg,
            subquery.targetid,
            subquery.targettype,
            subquery.targetname,
            subquery.targetdisplayname,
            subquery.targetdescription,
            subquery.targettitle,
            subquery.targetorg,
            subquery.outcome,
            subquery.stagenumber,
            subquery.campaigniteration,
            subquery.stage,
            subquery.approver,
            subquery.certname
            FROM ( SELECT DISTINCT macc.oid AS campaignoid,
                    maccas.cid AS caseoid,
                    macw.cid AS workitemid,
                    maccas.objectreftargetoid AS objectid,
                    maccas.objectreftargettype::text AS objecttype,
                        CASE
                            WHEN maccas.objectreftargettype = 'USER'::objecttype THEN ( SELECT mu0.namenorm
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.objectreftargetoid)
                            WHEN maccas.objectreftargettype = 'ORG'::objecttype THEN ( SELECT mo0.namenorm
                               FROM m_org mo0
                              WHERE mo0.oid = maccas.objectreftargetoid)
                            WHEN maccas.objectreftargettype = 'ROLE'::objecttype THEN ( SELECT mr0.namenorm
                               FROM m_role mr0
                              WHERE mr0.oid = maccas.objectreftargetoid)
                            ELSE NULL::text
                        END AS objectname,
                        CASE
                            WHEN maccas.objectreftargettype = 'USER'::objecttype THEN ( SELECT mu0.fullnameorig
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.objectreftargetoid)
                            WHEN maccas.objectreftargettype = 'ORG'::objecttype THEN ( SELECT ((convert_from(mo0.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'displayName'::text
                               FROM m_org mo0
                              WHERE mo0.oid = maccas.objectreftargetoid)
                            WHEN maccas.objectreftargettype = 'ROLE'::objecttype THEN ( SELECT ((convert_from(mr0.fullobject, 'UTF8'::name)::json ->> 'role'::text)::json) ->> 'displayName'::text
                               FROM m_role mr0
                              WHERE mr0.oid = maccas.objectreftargetoid)
                            ELSE NULL::text
                        END AS objectdisplayname,
                        CASE
                            WHEN maccas.objectreftargettype = 'USER'::objecttype THEN ( SELECT ((convert_from(mu0.fullobject, 'UTF8'::name)::json ->> 'user'::text)::json) ->> 'description'::text
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.objectreftargetoid)
                            WHEN maccas.objectreftargettype = 'ORG'::objecttype THEN ( SELECT ((convert_from(mo0.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'description'::text
                               FROM m_org mo0
                              WHERE mo0.oid = maccas.objectreftargetoid)
                            WHEN maccas.objectreftargettype = 'ROLE'::objecttype THEN ( SELECT ((convert_from(mr0.fullobject, 'UTF8'::name)::json ->> 'role'::text)::json) ->> 'description'::text
                               FROM m_role mr0
                              WHERE mr0.oid = maccas.objectreftargetoid)
                            ELSE NULL::text
                        END AS objectdescription,
                        CASE
                            WHEN maccas.objectreftargettype = 'USER'::objecttype THEN ( SELECT mu0.titleorig
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.objectreftargetoid)
                            ELSE NULL::text
                        END AS objecttitle,
                        CASE
                            WHEN maccas.objectreftargettype = 'USER'::objecttype THEN ( SELECT (mu0.organizations -> 0) ->> 'o'::text
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.objectreftargetoid)
                            ELSE NULL::text
                        END AS objectorg,
                    maccas.targetreftargetoid AS targetid,
                    maccas.targetreftargettype AS targettype,
                        CASE
                            WHEN maccas.targetreftargettype = 'USER'::objecttype THEN ( SELECT mu0.namenorm
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.targetreftargetoid)
                            WHEN maccas.targetreftargettype = 'ORG'::objecttype THEN ( SELECT mo0.namenorm
                               FROM m_org mo0
                              WHERE mo0.oid = maccas.targetreftargetoid)
                            WHEN maccas.targetreftargettype = 'ROLE'::objecttype THEN ( SELECT mr0.namenorm
                               FROM m_role mr0
                              WHERE mr0.oid = maccas.targetreftargetoid)
                            ELSE NULL::text
                        END AS targetname,
                        CASE
                            WHEN maccas.targetreftargettype = 'USER'::objecttype THEN ( SELECT mu0.fullnameorig
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.targetreftargetoid)
                            WHEN maccas.targetreftargettype = 'ORG'::objecttype THEN ( SELECT ((convert_from(mo0.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'displayName'::text
                               FROM m_org mo0
                              WHERE mo0.oid = maccas.targetreftargetoid)
                            WHEN maccas.targetreftargettype = 'ROLE'::objecttype THEN ( SELECT ((convert_from(mr0.fullobject, 'UTF8'::name)::json ->> 'role'::text)::json) ->> 'displayName'::text
                               FROM m_role mr0
                              WHERE mr0.oid = maccas.targetreftargetoid)
                            ELSE NULL::text
                        END AS targetdisplayname,
                        CASE
                            WHEN maccas.targetreftargettype = 'USER'::objecttype THEN ( SELECT ((convert_from(mu0.fullobject, 'UTF8'::name)::json ->> 'user'::text)::json) ->> 'description'::text
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.targetreftargetoid)
                            WHEN maccas.targetreftargettype = 'ORG'::objecttype THEN ( SELECT ((convert_from(mo0.fullobject, 'UTF8'::name)::json ->> 'org'::text)::json) ->> 'description'::text
                               FROM m_org mo0
                              WHERE mo0.oid = maccas.targetreftargetoid)
                            WHEN maccas.targetreftargettype = 'ROLE'::objecttype THEN ( SELECT ((convert_from(mr0.fullobject, 'UTF8'::name)::json ->> 'role'::text)::json) ->> 'description'::text
                               FROM m_role mr0
                              WHERE mr0.oid = maccas.targetreftargetoid)
                            ELSE NULL::text
                        END AS targetdescription,
                        CASE
                            WHEN maccas.targetreftargettype = 'USER'::objecttype THEN ( SELECT mu0.titleorig
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.targetreftargetoid)
                            ELSE NULL::text
                        END AS targettitle,
                        CASE
                            WHEN maccas.targetreftargettype = 'USER'::objecttype THEN ( SELECT (mu0.organizations -> 0) ->> 'o'::text
                               FROM m_user mu0
                              WHERE mu0.oid = maccas.targetreftargetoid)
                            ELSE NULL::text
                        END AS targetorg,
                    macw.outcome,
                    maccas.stagenumber,
                    maccas.campaigniteration,
                    ((convert_from(macc.fullobject, 'UTF8'::name)::json ->> 'accessCertificationCampaign'::text)::json) ->> 'stage'::text AS stage,
                    macwa.targetoid AS approver,
                    macc.nameorig AS certname
                   FROM m_access_cert_case maccas,
                    m_access_cert_campaign macc,
                    m_access_cert_wi macw,
                    m_access_cert_wi_assignee macwa
                  WHERE maccas.owneroid = macc.oid AND macw.owneroid = maccas.owneroid AND macwa.owneroid = macw.owneroid AND macw.accesscertcasecid = maccas.cid AND macwa.accesscertworkitemcid = macw.cid AND macw.stagenumber = maccas.stagenumber AND macw.outcome IS NULL) subquery
            """;

    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("campaignOid", "campaignoid"),
            Map.entry("caseOid", "caseoid"),
            Map.entry("workItemId", "workitemid"),
            Map.entry("objectId", "objectid"),
            Map.entry("objectType", "objecttype"),
            Map.entry("objectName", "objectname"),
            Map.entry("objectDisplayName", "objectdisplayname"),
            Map.entry("objectDescription", "objectdescription"),
            Map.entry("objectTitle", "objecttitle"),
            Map.entry("objectOrg", "objectorg"),
            Map.entry("targetId", "targetid"),
            Map.entry("targetType", "targettype"),
            Map.entry("targetName", "targetname"),
            Map.entry("targetDisplayName", "targetdisplayname"),
            Map.entry("targetDescription", "targetdescription"),
            Map.entry("targetTitle", "targettitle"),
            Map.entry("targetOrg", "targetorg"),
            Map.entry("outcome", "outcome"),
            Map.entry("stageNumber", "stagenumber"),
            Map.entry("campaignIteration", "campaigniteration"),
            Map.entry("stage", "stage"),
            Map.entry("approver", "approver"),
            Map.entry("certName", "certname")
    );

    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("campaignoid", "subquery.campaignoid"),
            Map.entry("caseoid", "subquery.caseoid"),
            Map.entry("workitemid", "subquery.workitemid"),
            Map.entry("objectid", "subquery.objectid"),
            Map.entry("objecttype", "subquery.objecttype"),
            Map.entry("objectname", "subquery.objectname"),
            Map.entry("objectdisplayname", "subquery.objectdisplayname"),
            Map.entry("objectdescription", "subquery.objectdescription"),
            Map.entry("objecttitle", "subquery.objecttitle"),
            Map.entry("objectorg", "subquery.objectorg"),
            Map.entry("targetid", "subquery.targetid"),
            Map.entry("targettype", "subquery.targettype"),
            Map.entry("targetname", "subquery.targetname"),
            Map.entry("targetdisplayname", "subquery.targetdisplayname"),
            Map.entry("targetdescription", "subquery.targetdescription"),
            Map.entry("targettitle", "subquery.targettitle"),
            Map.entry("targetorg", "subquery.targetorg"),
            Map.entry("outcome", "subquery.outcome"),
            Map.entry("stagenumber", "subquery.stagenumber"),
            Map.entry("campaigniteration", "subquery.campaigniteration"),
            Map.entry("stage", "subquery.stage"),
            Map.entry("approver", "subquery.approver"),
            Map.entry("certname", "subquery.certname")
    );

    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

    public AccessCertActiveRequestJdbcRepository(JdbcTemplate jdbc) {
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

    @Override protected boolean baseHasWhere() { return true; }

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
