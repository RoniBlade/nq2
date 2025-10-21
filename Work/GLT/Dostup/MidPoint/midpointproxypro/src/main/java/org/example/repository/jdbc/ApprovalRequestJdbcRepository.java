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
public class ApprovalRequestJdbcRepository extends BaseJdbcRepo<Map<String,Object>> {
    public ApprovalRequestJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String BASE_SELECT = """
        SELECT row_number() OVER () AS id,
               mc.oid AS caseoid,
               mcw.cid AS work_itemid,
               mc.db_created AS casedatecreate,
               mc.objectreftargetoid AS useroid,
               mu.nameorig AS username,
               mu.fullnameorig AS userfullname,
               mu.titleorig AS usertitle,
               (mu.organizations -> 0) ->> 'o'::text AS userorganization,
               mu.emailaddress AS useremail,
               mu.telephonenumber AS userphone,
               mc.requestorreftargetoid AS requesteroid,
               mu1.nameorig AS requestername,
               mu1.fullnameorig AS requesterfullname,
               mu1.titleorig AS requestertitle,
               (mu1.organizations -> 0) ->> 'o'::text AS asrequesterorganization,
               mu1.emailaddress AS requesteremail,
               mu1.telephonenumber AS requesterphone,
               mc.targetreftargetoid AS objectoid,
               mo.namenorm AS objectname,
               ((convert_from(mo.fullobject, 'UTF8'::name)::json ->> 'role')::json) ->> 'displayName' AS objectdisplayname,
               ((convert_from(mo.fullobject, 'UTF8'::name)::json ->> 'role')::json) ->> 'description' AS objectdescription,
               mo.objecttype::text AS objecttype,
               mc.state,
               mot.oid AS oidapprover,
               mot.namenorm AS nameapprover
        FROM m_case_wi_assignee mcwa,
             m_case_wi mcw,
             m_object mot,
             m_case mc,
             m_user mu,
             m_user mu1,
             m_object mo
        WHERE mcwa.owneroid = mcw.owneroid
          AND mcwa.workitemcid = mcw.cid
          AND mcwa.owneroid = mc.oid
          AND mcwa.targetoid = mot.oid
          AND mc.targetreftargetoid = mo.oid
          AND mcw.outcome IS NULL
          AND mc.objectreftargetoid = mu.oid
          AND mc.requestorreftargetoid = mu1.oid
          AND mc.state <> 'closed'::text
        """;

    // === Маппинг алиасов → выражения ДО AS ===
    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("caseoid", "mc.oid"),
            Map.entry("work_itemid", "mcw.cid"),      Map.entry("work_item_id", "mcw.cid"),
            Map.entry("casedatecreate", "mc.db_created"),
            Map.entry("useroid", "mc.objectreftargetoid"),
            Map.entry("username", "mu.nameorig"),
            Map.entry("userfullname", "mu.fullnameorig"),
            Map.entry("usertitle", "mu.titleorig"),
            Map.entry("userorganization", "(mu.organizations -> 0) ->> 'o'::text"),
            Map.entry("useremail", "mu.emailaddress"),
            Map.entry("userphone", "mu.telephonenumber"),
            Map.entry("requesteroid", "mc.requestorreftargetoid"),
            Map.entry("requestername", "mu1.nameorig"),
            Map.entry("requesterfullname", "mu1.fullnameorig"),
            Map.entry("requestertitle", "mu1.titleorig"),
            Map.entry("asrequesterorganization", "(mu1.organizations -> 0) ->> 'o'::text"),
            Map.entry("requesteremail", "mu1.emailaddress"),
            Map.entry("requesterphone", "mu1.telephonenumber"),
            Map.entry("objectoid", "mc.targetreftargetoid"),
            Map.entry("objectname", "mo.namenorm"),
            Map.entry("objectdisplayname", "((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json) ->> 'displayName'"),
            Map.entry("objectdescription", "((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json) ->> 'description'"),
            Map.entry("objecttype", "mo.objecttype::text"),
            Map.entry("state", "mc.state"),
            Map.entry("oidapprover", "mot.oid"),
            Map.entry("nameapprover", "mot.namenorm")
    );

    private static String norm(String s) { return s == null ? null : s.replace("_", "").toLowerCase(); }

    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("id", "id"), // не пригодится во внутреннем where
            Map.entry("caseOid", "caseoid"),
            Map.entry("workItemId", "work_itemid"),
            Map.entry("caseDateCreate", "casedatecreate"),
            Map.entry("userOid", "useroid"),
            Map.entry("userName", "username"),
            Map.entry("userFullName", "userfullname"),
            Map.entry("userTitle", "usertitle"),
            Map.entry("userOrganization", "userorganization"),
            Map.entry("userEmail", "useremail"),
            Map.entry("userPhone", "userphone"),
            Map.entry("requesterOid", "requesteroid"),
            Map.entry("requesterName", "requestername"),
            Map.entry("requesterFullName", "requesterfullname"),
            Map.entry("requesterTitle", "requestertitle"),
            Map.entry("asRequesterOrganization", "asrequesterorganization"),
            Map.entry("requesterEmail", "requesteremail"),
            Map.entry("requesterPhone", "requesterphone"),
            Map.entry("objectOid", "objectoid"),
            Map.entry("objectName", "objectname"),
            Map.entry("objectDisplayName", "objectdisplayname"),
            Map.entry("objectDescription", "objectdescription"),
            Map.entry("objectType", "objecttype"),
            Map.entry("state", "state"),
            Map.entry("oidApprover", "oidapprover"),
            Map.entry("nameApprover", "nameapprover")
    );

    private String resolve(String field) {
        String n = norm(field);
        // DTO -> alias
        for (var e : DTO_TO_ALIAS.entrySet()) {
            if (Objects.equals(norm(e.getKey()), n)) {
                String alias = e.getValue();
                String expr = ALIAS_TO_EXPR.get(norm(alias));
                return expr != null ? expr : alias;
            }
        }
        // alias -> expr
        for (var e : ALIAS_TO_EXPR.entrySet()) {
            if (Objects.equals(norm(e.getKey()), n)) return e.getValue();
        }
        return field;
    }

    @Override protected String baseSelect() { return BASE_SELECT; }

    @Override protected boolean baseHasWhere() { return true; }

    @Override protected Function<String, String> fieldResolver() { return this::resolve; }

    @Override protected Map<String, Object> mapRow(Map<String, Object> row) { return row; }

    public Page<Map<String, Object>> findAll(Pageable pageable) {
        return super.page(Collections.emptyList(), pageable);
    }

    public Page<Map<String, Object>> findByFilters(List<FilterNode> filters, Pageable pageable) {
        return super.page(filters, pageable);
    }
}
