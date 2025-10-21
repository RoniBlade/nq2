package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.AccountsDto;
import org.example.model.filter.FilterNode;
import org.example.repository.BaseJdbcRepo;
import org.example.util.SqlBuilder;
import org.example.util.filter.FilterSqlBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.function.Function;

import static org.example.util.JdbcRows.norm;

@Repository
@Slf4j
public class AccountsJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {


    private static final String BASE_SELECT = """
                select
                ms.oid,
                ms.objecttype,
                ms.nameorig,
                ms.namenorm,
                ms.fullobject,
                ms.tenantreftargetoid,
                ms.tenantreftargettype,
                ms.tenantrefrelationid,
                ms.lifecyclestate,
                ms.cidseq,
                ms.version,
                ms.policysituations,
                ms.subtypes,
                ms.fulltextinfo,
                ms.ext,
                ms.creatorreftargetoid,
                ms.creatorreftargettype,
                ms.creatorrefrelationid,
                ms.createchannelid,
                ms.createtimestamp,
                ms.modifierreftargetoid,
                ms.modifierreftargettype,
                ms.modifierrefrelationid,
                ms.modifychannelid,
                ms.modifytimestamp,
                ms.db_created,
                ms.db_modified,
                ms.objectclassid,
                ms.resourcereftargetoid,
                ms.resourcereftargettype,
                ms.resourcerefrelationid,
                ms.intent,
                ms.tag,
                ms.kind,
                ms.dead,
                ms.exist,
                ms.fullsynchronizationtimestamp,
                ms.pendingoperationcount,
                ms.primaryidentifiervalue,
                ms.synchronizationsituation,
                ms.synchronizationtimestamp,
                ms.attributes,
                ms.correlationstarttimestamp,
                ms.correlationendtimestamp,
                ms.correlationcaseopentimestamp,
                ms.correlationcaseclosetimestamp,
                ms.correlationsituation,
                ms.disablereasonid,
                ms.enabletimestamp,
                ms.disabletimestamp,
                ms.lastlogintimestamp,
                glt_get_shadow_displayname(ms.oid::text) AS v_displayname,
                mrp.owneroid AS user_oid
                FROM m_shadow ms,
                m_ref_projection mrp
                WHERE ms.oid = mrp.targetoid
            """;

    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("oid", "oid"),
            Map.entry("objectType", "objecttype"),
            Map.entry("nameOrig", "nameorig"),
            Map.entry("nameNorm", "namenorm"),
            Map.entry("fullObject", "fullobject"),
            Map.entry("tenantRefTargetOid", "tenantreftargetoid"),
            Map.entry("tenantRefTargetType", "tenantreftargettype"),
            Map.entry("tenantRefRelationId", "tenantrefrelationid"),
            Map.entry("lifeCycleState", "lifecyclestate"),
            Map.entry("cidSeq", "cidseq"),
            Map.entry("version", "version"),
            Map.entry("policySituations", "policysituations"),
            Map.entry("subTypes", "subtypes"),
            Map.entry("fullTextInfo", "fulltextinfo"),
            Map.entry("ext", "ext"),
            Map.entry("creatorRefTargetOid", "creatorreftargetoid"),
            Map.entry("creatorRefTargetType", "creatorreftargettype"),
            Map.entry("creatorRefRelationId", "creatorrefrelationid"),
            Map.entry("createChannelId", "createchannelid"),
            Map.entry("createTimestamp", "createtimestamp"),
            Map.entry("modifierRefTargetOid", "modifierreftargetoid"),
            Map.entry("modifierRefTargetType", "modifierreftargettype"),
            Map.entry("modifierRefRelationId", "modifierrefrelationid"),
            Map.entry("modifyChannelId", "modifychannelid"),
            Map.entry("modifyTimestamp", "modifytimestamp"),
            Map.entry("dbCreated", "db_created"),
            Map.entry("dbModified", "db_modified"),
            Map.entry("objectClassId", "objectclassid"),
            Map.entry("resourceRefTargetOid", "resourcereftargetoid"),
            Map.entry("resourceRefTargetType", "resourcereftargettype"),
            Map.entry("resourceRefRelationId", "resourcerefrelationid"),
            Map.entry("intent", "intent"),
            Map.entry("tag", "tag"),
            Map.entry("kind", "kind"),
            Map.entry("dead", "dead"),
            Map.entry("exist", "exist"),
            Map.entry("fullSynchronizationTimestamp", "fullsynchronizationtimestamp"),
            Map.entry("pendingOperationCount", "pendingoperationcount"),
            Map.entry("primaryIdentifierValue", "primaryidentifiervalue"),
            Map.entry("synchronizationSituation", "synchronizationsituation"),
            Map.entry("synchronizationTimestamp", "synchronizationtimestamp"),
            Map.entry("attributes", "attributes"),
            Map.entry("correlationStartTimestamp", "correlationstarttimestamp"),
            Map.entry("correlationEndTimestamp",  "correlationendtimestamp"),
            Map.entry("correlationCaseOpenTimestamp","correlationcaseopentimestamp"),
            Map.entry("correlationCaseCloseTimestamp", "correlationcaseclosetimestamp"),
            Map.entry("correlationSituation", "correlationsituation"),
            Map.entry("disableReasonId", "disablereasonid"),
            Map.entry("enableTimestamp", "enabletimestamp"),
            Map.entry("disableTimestamp", "disabletimestamp"),
            Map.entry("lastLoginTimestamp", "lastlogintimestamp"),
            Map.entry("vDisplayName", "v_displayname"),
            Map.entry("user_oid", "user_oid")
    );

    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("oid", "ms.oid"),
            Map.entry("objectType", "ms.objecttype"),
            Map.entry("nameOrig", "ms.nameorig"),
            Map.entry("nameNorm", "ms.namenorm"),
            Map.entry("fullObject", "ms.fullobject"),
            Map.entry("tenantRefTargetOid", "ms.tenantreftargetoid"),
            Map.entry("tenantRefTargetType", "ms.tenantreftargettype"),
            Map.entry("tenantRefRelationId", "ms.tenantrefrelationid"),
            Map.entry("lifeCycleState", "ms.lifecyclestate"),
            Map.entry("cidSeq", "ms.cidseq"),
            Map.entry("version", "ms.version"),
            Map.entry("policySituations", "ms.policysituations"),
            Map.entry("subTypes", "ms.subtypes"),
            Map.entry("fullTextInfo", "ms.fulltextinfo"),
            Map.entry("ext", "ms.ext"),
            Map.entry("creatorRefTargetOid", "ms.creatorreftargetoid"),
            Map.entry("creatorRefTargetType", "ms.creatorreftargettype"),
            Map.entry("creatorRefRelationId", "ms.creatorrefrelationid"),
            Map.entry("createChannelId", "ms.createchannelid"),
            Map.entry("createTimestamp", "ms.createtimestamp"),
            Map.entry("modifierRefTargetOid", "ms.modifierreftargetoid"),
            Map.entry("modifierRefTargetType", "ms.modifierreftargettype"),
            Map.entry("modifierRefRelationId", "ms.modifierrefrelationid"),
            Map.entry("modifyChannelId", "ms.modifychannelid"),
            Map.entry("modifyTimestamp", "ms.modifytimestamp"),
            Map.entry("dbCreated", "ms.db_created"),
            Map.entry("dbModified", "ms.db_modified"),
            Map.entry("objectClassId", "ms.objectclassid"),
            Map.entry("resourceRefTargetOid", "ms.resourcereftargetoid"),
            Map.entry("resourceRefTargetType", "ms.resourcereftargettype"),
            Map.entry("resourceRefRelationId", "ms.resourcerefrelationid"),
            Map.entry("intent", "ms.intent"),
            Map.entry("tag", "ms.tag"),
            Map.entry("kind", "ms.kind"),
            Map.entry("dead", "ms.dead"),
            Map.entry("exist", "ms.exist"),
            Map.entry("fullSynchronizationTimestamp", "ms.fullsynchronizationtimestamp"),
            Map.entry("pendingOperationCount", "ms.pendingoperationcount"),
            Map.entry("primaryIdentifierValue", "ms.primaryidentifiervalue"),
            Map.entry("synchronizationSituation", "ms.synchronizationsituation"),
            Map.entry("synchronizationTimestamp", "ms.synchronizationtimestamp"),
            Map.entry("attributes", "attributes"),
            Map.entry("correlationStartTimestamp", "ms.correlationstarttimestamp"),
            Map.entry("correlationEndTimestamp",  "ms.correlationendtimestamp"),
            Map.entry("correlationCaseOpenTimestamp","ms.correlationcaseopentimestamp"),
            Map.entry("correlationCaseCloseTimestamp", "ms.correlationcaseclosetimestamp"),
            Map.entry("correlationSituation", "ms.correlationsituation"),
            Map.entry("disableReasonId", "ms.disablereasonid"),
            Map.entry("enableTimestamp", "ms.enabletimestamp"),
            Map.entry("disableTimestamp", "ms.disabletimestamp"),
            Map.entry("lastLoginTimestamp", "ms.lastlogintimestamp"),
            Map.entry("vDisplayName", "glt_get_shadow_displayname(ms.oid::text)"),
            Map.entry("user_oid", "mrp.owneroid")
    );

    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

    public AccountsJdbcRepository(JdbcTemplate jdbc) {
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
