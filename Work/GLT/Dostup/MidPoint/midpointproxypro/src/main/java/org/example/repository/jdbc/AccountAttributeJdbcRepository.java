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
public class AccountAttributeJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {

    public AccountAttributeJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String BASE_SELECT = """
            SELECT
                    ms.oid,
                    replace(replace(mei.itemname, 'http://midpoint.evolveum.com/xml/ns/public/resource/instance-3#'::text, ''::text), 'http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3#'::text, ''::text) AS attr_name,
                    postgres.glt_get_attribute_value(xx.value) AS attr_value,
                        CASE
                            WHEN (((convert_from(ms.fullobject, 'UTF8'::name)::json ->> 'shadow'::text)::json) ->> 'referenceAttributes'::text) IS NULL OR length(((convert_from(ms.fullobject, 'UTF8'::name)::json ->> 'shadow'::text)::json) ->> 'referenceAttributes'::text) = 2 THEN false
                            ELSE true
                        END AS entitlements
                   FROM m_shadow ms,
                    LATERAL json_each_text(ms.attributes::json) xx(key, value),
                    m_ext_item mei
                  WHERE mei.id = xx.key::integer
        """;

    // ===== нормализация имён (snake/camel → без подчёркиваний, lower) =====
    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

    // Алиас -> выражение ДО AS (для WHERE). Здесь всё простое: колонки из reqq.*
    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("oid", "ms.oid"),
            Map.entry("attr_name", "replace(replace(mei.itemname, 'http://midpoint.evolveum.com/xml/ns/public/resource/instance-3#'::text, ''::text), 'http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3#'::text, ''::text)"),
            Map.entry("attr_value", "postgres.glt_get_attribute_value(xx.value)"),
            Map.entry("entitlements", "CASE\n"
                    + "            WHEN (((convert_from(ms.fullobject, 'UTF8'::name)::json ->> 'shadow'::text)::json) ->> 'referenceAttributes'::text) IS NULL OR length(((convert_from(ms.fullobject, 'UTF8'::name)::json ->> 'shadow'::text)::json) ->> 'referenceAttributes'::text) = 2 THEN false\n"
                    + "            ELSE true\n"
                    + "        END")
            // id — это window-функция (row_number) и в WHERE не используется
    );

    // DTO-имена (camelCase) -> алиасы из SELECT
    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("oid", "oid"),
            Map.entry("attr_Name", "attr_name"),
            Map.entry("attr_Value", "attr_value"),
            Map.entry("entitlements", "entitlements")
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
