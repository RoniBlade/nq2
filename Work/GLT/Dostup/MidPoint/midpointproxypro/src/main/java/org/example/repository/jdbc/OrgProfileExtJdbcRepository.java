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
public class OrgProfileExtJdbcRepository extends BaseJdbcRepo<Map<String,Object>> {

    private static final String BASE_SELECT = """
        SELECT
             m_org.oid,
             xx.value AS extattrvalue,
             replace(mei.itemname, public.glt_get_config_value('shema\\ext_shema'::text), ''::text) AS extattrname,
             mei.cardinality,
             split_part(mei.valuetype, '#'::text, 2) AS type,
                 CASE
                     WHEN mei.valuetype ~~ (public.glt_get_config_value('shema\\ext_shema'::text) || '%'::text) THEN 'EXT'::text
                     ELSE 'BASIC'::text
                 END AS contanertype
            FROM m_org
              LEFT JOIN LATERAL json_each_text(m_org.ext::json) xx(key, value) ON true
              LEFT JOIN m_ext_item mei ON mei.id = xx.key::integer
""";

    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("oid", "oid"),
            Map.entry("extAttrValue", "extattrvalue"),
            Map.entry("extAttrName", "extarrename"),
            Map.entry("cardinality", "cardinality"),
            Map.entry("type", "type"),
            Map.entry("contanerType", "contanertype")

    );

    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("oid", "m_org.oid"),
            Map.entry("extattrvalue", "xx.value"),
            Map.entry("extattrname", "replace(mei.itemname, public.glt_get_config_value('shema\\ext_shema'::text), ''::text)"),
            Map.entry("cardinality", "mei.cardinality"),
            Map.entry("type", "split_part(mei.valuetype, '#'::text, 2)"),
            Map.entry("contanertype", "CASE\n"
                    + "    WHEN mei.valuetype ~~ (public.glt_get_config_value('shema\\ext_shema'::text) || '%'::text) THEN 'EXT'::text\n"
                    + "    ELSE 'BASIC'::text\n"
                    + "END")
    );

    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

    public OrgProfileExtJdbcRepository(JdbcTemplate jdbc) {
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
