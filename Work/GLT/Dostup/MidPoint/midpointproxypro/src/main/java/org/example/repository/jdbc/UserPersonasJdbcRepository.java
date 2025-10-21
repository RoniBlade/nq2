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
public class UserPersonasJdbcRepository extends BaseJdbcRepo<Map<String, Object>> {

    public UserPersonasJdbcRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String BASE_SELECT = """
            SELECT
                      mrp.owneroid,
                      mrp.targetoid,
                      own.nameorig AS own_nameorig,
                      own.namenorm AS own_namenorm,
                      ((convert_from(own.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'fullName'::text AS own_fullname,
                      per.nameorig AS per_nameorig,
                      per.namenorm AS per_namenorm,
                      ((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'fullName'::text AS per_fullname,
                      ((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'emailAddress'::text AS per_emailaddress,
                      ((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'personalNumber'::text AS per_personalnumber,
                      glt_get_user_displayname(per.oid::text) AS vdisplayname,
                      ((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::text)::json) ->> 'description'::text AS description,
                      ma.oid AS target_archetype_oid,
                      ma.nameorig AS target_archetype_nameorig,
                      ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'cssClass'::text AS archetype_icon_class,
                      ((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'color'::text AS archetype_icon_color
                     FROM m_ref_persona mrp
                       JOIN m_user own ON mrp.owneroid = own.oid
                       JOIN m_user per ON mrp.targetoid = per.oid
                       LEFT JOIN m_archetype ma ON ((((((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'archetypeRef'::text)::jsonb) ->> 'oid'::text)::uuid) = ma.oid
        """;

    // ===== нормализация имён (snake/camel → без подчёркиваний, lower) =====
    private static String norm(String s) { return s == null ? null : s.toLowerCase(); }

    // Алиас -> выражение ДО AS (для WHERE). Здесь всё простое: колонки из reqq.*
    private static final Map<String, String> ALIAS_TO_EXPR = Map.ofEntries(
            Map.entry("owneroid", "mrp.owneroid"),
            Map.entry("targetoid", "mrp.targetoid"),
            Map.entry("own_nameorig", "own.nameorig"),
            Map.entry("own_namenorm", "own.namenorm"),
            Map.entry("own_fullname", "((convert_from(own.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'fullName'::text"),
            Map.entry("per_nameorig", "per.nameorig"),
            Map.entry("per_namenorm", "per.namenorm"),
            Map.entry("per_fullname", "((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'fullName'::text"),
            Map.entry("per_emailaddress", "((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'emailAddress'::text"),
            Map.entry("per_personalnumber", "((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::character varying::text)::json) ->> 'personalNumber'::text"),
            Map.entry("vdisplayname", "glt_get_user_displayname(per.oid::text)"),
            Map.entry("description", "((convert_from(per.fullobject, 'UTF8'::name)::json ->> 'user'::text)::json) ->> 'description'::text"),
            Map.entry("target_archetype_oid", "ma.oid"),
            Map.entry("target_archetype_nameorig", "ma.nameorig"),
            Map.entry("archetype_icon_class", "((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'cssClass'::text"),
            Map.entry("archetype_icon_color", "((((((((convert_from(ma.fullobject, 'UTF8'::name)::json ->> 'archetype'::text)::json) ->> 'archetypePolicy'::text)::json) ->> 'display'::text)::json) ->> 'icon'::text)::json) ->> 'color'::text")
            // id — это window-функция (row_number) и в WHERE не используется
    );

    // DTO-имена (camelCase) -> алиасы из SELECT
    private static final Map<String, String> DTO_TO_ALIAS = Map.ofEntries(
            Map.entry("ownerOid", "owneroid"),
            Map.entry("targetOid", "targetoid"),
            Map.entry("own_nameOrig", "own_nameorig"),
            Map.entry("own_nameNorm", "own_namenorm"),
            Map.entry("own_fullName", "own_fullname"),
            Map.entry("per_nameOrig", "per_nameorig"),
            Map.entry("per_nameNorm", "per_namenorm"),
            Map.entry("per_fullName", "per_fullname"),
            Map.entry("per_emailAddress", "per_emailaddress"),
            Map.entry("per_personalNumber", "per_personalnumber"),
            Map.entry("vDisplayName", "vdisplayname"),
            Map.entry("description", "description"),
            Map.entry("target_archetype_oid", "target_archetype_oid"),
            Map.entry("target_archetype_nameOrig", "target_archetype_nameorig"),
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
