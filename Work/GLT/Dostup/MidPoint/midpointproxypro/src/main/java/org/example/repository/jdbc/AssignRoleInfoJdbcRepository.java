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
public class AssignRoleInfoJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {

    public AssignRoleInfoJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String BASE_SELECT = """
                SELECT
                    ff.owneroid,
                    ff.type,
                    ff.containertype,
                    ff.targetoid,
                    ff.object_description,
                    ff.kind,
                    ff.intent
                   FROM ( SELECT ma.owneroid,
                            ma.targetreftargettype AS type,
                            ma.containertype,
                            ma.targetreftargetoid AS targetoid,
                                CASE
                                    WHEN ma.targetreftargettype::text = 'ROLE'::text THEN ( SELECT glt_get_role_displayname(mm.oid::text) AS glt_get_role_displayname
                                       FROM m_role mm
                                      WHERE mm.oid = ma.targetreftargetoid)
                                    WHEN ma.targetreftargettype::text = 'ORG'::text THEN ( SELECT glt_get_org_displayname(mo.oid::text) AS glt_get_org_displayname
                                       FROM m_org mo
                                      WHERE mo.oid = ma.targetreftargetoid)
                                    WHEN ma.targetreftargettype::text = 'SERVICE'::text THEN ( SELECT ms.nameorig
                                       FROM m_service ms
                                      WHERE ms.oid = ma.targetreftargetoid)
                                    WHEN ma.targetreftargettype::text = 'ARCHETYPE'::text THEN ( SELECT glt_get_archetype_displayname(maa.oid::text) AS glt_get_archetype_displayname
                                       FROM m_archetype maa
                                      WHERE maa.oid = ma.targetreftargetoid)
                                    WHEN ma.targetreftargettype::text = 'POLICY'::text THEN ( SELECT mp.nameorig
                                       FROM m_policy mp
                                      WHERE mp.oid = ma.targetreftargetoid)
                                    ELSE NULL::text
                                END AS object_description,
                            ''::text AS kind,
                            ''::text AS intent
                           FROM m_assignment ma
                          WHERE ma.resourcereftargetoid IS NULL AND (ma.targetreftargettype = ANY (ARRAY['ROLE'::objecttype, 'ORG'::objecttype, 'SERVICE'::objecttype, 'ARCHETYPE'::objecttype, 'POLICY'::objecttype]))
                        UNION ALL
                         SELECT ma.owneroid,
                            ma.resourcereftargettype AS type,
                            ma.containertype,
                            ma.resourcereftargetoid AS targetoid,
                                CASE
                                    WHEN ma.resourcereftargettype::text = 'RESOURCE'::text THEN ( SELECT m.nameorig
                                       FROM m_resource m
                                      WHERE m.oid = ma.resourcereftargetoid)
                                    ELSE NULL::text
                                END AS object_description,
                            ((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'construction'::text)::json) ->> 'kind'::text AS kind,
                            ((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'construction'::text)::json) ->> 'intent'::text AS intent
                           FROM m_assignment ma
                          WHERE ma.resourcereftargetoid IS NOT NULL AND (ma.targetreftargettype = ANY (ARRAY['ROLE'::objecttype, 'ORG'::objecttype, 'SERVICE'::objecttype, 'ARCHETYPE'::objecttype, 'POLICY'::objecttype]))) ff
            """;

    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
//            Map.entry("id", ""),
            Map.entry("owneroid", "ma.owneroid"),
            Map.entry("type", "ma.resourcereftargettype"),
            Map.entry("containertype", "ma.containertype"),
            Map.entry("targetoid", "ma.resourcereftargetoid"),
            Map.entry("object_description", "CASE\n"
                    + "    WHEN ma.resourcereftargettype::text = 'RESOURCE'::text THEN ( SELECT m.nameorig\n"
                    + "       FROM m_resource m\n"
                    + "      WHERE m.oid = ma.resourcereftargetoid)\n"
                    + "    ELSE NULL::text\n"
                    + "END"),
            Map.entry("kind", "((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'construction'::text)::json) ->> 'kind'::text"),
            Map.entry("intent", "((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'assignment'::text)::json) ->> 'construction'::text)::json) ->> 'intent'::text")
            // id — это window-функция (row_number) и в WHERE не используется
    );

    // DTO-имена (camelCase) -> алиасы из SELECT
    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
//            Map.entry("id", "id"),
            Map.entry("ownerOid", "owneroid"),
            Map.entry("type", "type"),
            Map.entry("containerType", "containertype"),
            Map.entry("targetOid", "targetoid"),
            Map.entry("object_description", "object_description"),
            Map.entry("kind", "kind"),
            Map.entry("intent", "intent")
    );

    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

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
        return "ff." + cleaned;
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
