package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.util.SqlBuilder;
import org.example.util.filter.FilterSqlBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ObjectInfoLiteJdbcRepository {

    private final JdbcTemplate jdbc;

    // БАЗОВАЯ ПРОЕКЦИЯ (как у тебя)
    private static final String BASE_SELECT = """
        SELECT row_number() OVER () AS id,
               mo.oid,
               mo.objecttype::text AS objecttype,
               CASE
                 WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'name'
                 WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'name'
                 WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'name'
                 WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'name'
                 ELSE NULL
               END AS object_name,
               CASE
                 WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'displayName'
                 WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'fullName'
                 WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'displayName'
                 WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'displayName'
                 ELSE NULL
               END AS object_fullname,
               CASE
                 WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'description'
                 WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'documentation'
                 WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'documentation'
                 WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'description'
                 ELSE NULL
               END AS object_description,
               CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'title' END AS user_title,
               CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'emailAddress' END AS user_emailaddress,
               CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'telephoneNumber' END AS user_telephonenumber,
               mu.photo AS user_photo
        FROM m_object mo
        LEFT JOIN m_user mu ON mo.oid = mu.oid
        """;

    // Маппинг алиасов полей фильтра -> выражения (как у тебя)
    private static final Map<String, String> FIELD_TO_EXPR = Map.ofEntries(
            Map.entry("oid", "mo.oid"),
            Map.entry("objecttype", "mo.objecttype::text"),
            Map.entry("objectType", "mo.objecttype::text"),
            Map.entry("object_type", "mo.objecttype::text"),

            Map.entry("object_name", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'name'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'name'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'name'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'name'
              ELSE NULL
            END
        """),
            Map.entry("objectName", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'name'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'name'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'name'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'name'
              ELSE NULL
            END
        """),
            Map.entry("objectname", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'name'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'name'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'name'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'name'
              ELSE NULL
            END
        """),

            Map.entry("object_fullname", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'displayName'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'fullName'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'displayName'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'displayName'
              ELSE NULL
            END
        """),
            Map.entry("objectFullName", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'displayName'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'fullName'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'displayName'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'displayName'
              ELSE NULL
            END
        """),
            Map.entry("objectfullname", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'displayName'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'FullName'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'displayName'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'displayName'
              ELSE NULL
            END
        """),

            Map.entry("object_description", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'description'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'documentation'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'documentation'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'description'
              ELSE NULL
            END
        """),
            Map.entry("objectDescription", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'description'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'documentation'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'documentation'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'description'
              ELSE NULL
            END
        """),
            Map.entry("objectdescription", """
            CASE
              WHEN mo.objecttype = 'ROLE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'role')::json)->>'description'
              WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'documentation'
              WHEN mo.objecttype = 'ORG'::objecttype  THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'org')::json)->>'documentation'
              WHEN mo.objecttype = 'SERVICE'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'service')::json)->>'description'
              ELSE NULL
            END
        """),

            Map.entry("user_title", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'title' END"),
            Map.entry("userTitle", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'title' END"),

            Map.entry("user_emailaddress", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'emailAddress' END"),
            Map.entry("userEmailAddress", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'emailAddress' END"),
            Map.entry("user_email", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'emailAddress' END"),
            Map.entry("useremailaddress", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'emailAddress' END"),

            Map.entry("user_telephonenumber", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'telephoneNumber' END"),
            Map.entry("userTelephoneNumber", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'telephoneNumber' END"),
            Map.entry("user_phone", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'telephoneNumber' END"),
            Map.entry("usertelephone", "CASE WHEN mo.objecttype = 'USER'::objecttype THEN ((convert_from(mo.fullobject,'UTF8')::json ->> 'user')::json)->>'telephoneNumber' END"),

            Map.entry("user_photo", "mu.photo"),
            Map.entry("photo", "mu.photo"),
            Map.entry("userPhoto", "mu.photo")
    );

    // === "Новая логика с OVER": источнику добавляем COUNT(*) OVER() ===
    private String wrapAsSourceWithTotal(String innerWhereSql) {
        // Внутри: BASE_SELECT (+ WHERE при наличии)
        // Снаружи: добавляем оконный total_count
        if (innerWhereSql == null || innerWhereSql.isBlank()) {
            return "SELECT src.*, COUNT(*) OVER() AS total_count FROM (" + BASE_SELECT + ") src";
        }
        return "SELECT src.*, COUNT(*) OVER() AS total_count FROM (" + BASE_SELECT + " WHERE " + innerWhereSql + ") src";
    }

    private String resolveField(String field) {
        return FIELD_TO_EXPR.getOrDefault(field, field);
    }

    public Page<Map<String, Object>> findAll(Pageable pageable) {
        String withTotal = wrapAsSourceWithTotal(null); // без фильтра, но уже с total_count

        var dataQ = SqlBuilder
                .selectFrom("(" + withTotal + ") data")
                .paginate(pageable)
                .build();

        log.debug("findAll (OVER) DATA SQL:\n{}\nPARAMS: {}", dataQ.sql(), dataQ.params());
        List<Map<String, Object>> rows = jdbc.queryForList(dataQ.sql(), dataQ.params().toArray());

        long total = extractTotalAndStrip(rows);

        return new PageImpl<>(rows, pageable, total);
    }

    public Page<Map<String, Object>> findByFilters(List<FilterNode> filters, Pageable pageable) {
        var wherePart = FilterSqlBuilder.newBuilder()
                .withFieldResolver(this::resolveField)
                .buildWhere(filters);

        String withTotal = wrapAsSourceWithTotal(wherePart.sql());

        var dataQ = SqlBuilder
                .selectFrom("(" + withTotal + ") s")
                .paginate(pageable)
                .build();

        // Порядок параметров: сначала внутренние WHERE, затем внешние (limit/offset)
        List<Object> dataParams = new ArrayList<>(wherePart.params().size() + dataQ.params().size());
        dataParams.addAll(wherePart.params());
        dataParams.addAll(dataQ.params());

        log.debug("findByFilters (OVER) DATA SQL:\n{}\nPARAMS: {}", dataQ.sql(), dataParams);
        List<Map<String, Object>> rows = jdbc.queryForList(dataQ.sql(), dataParams.toArray());

        long total = extractTotalAndStrip(rows);

        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * Забираем total_count из первой строки (если есть), удаляем служебный столбец из результата.
     */
    private long extractTotalAndStrip(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return 0L;
        Object v = rows.get(0).get("total_count");
        long total = (v instanceof Number) ? ((Number) v).longValue() : 0L;
        // подчистим колонку в каждой записи
        for (Map<String, Object> m : rows) {
            m.remove("total_count");
        }
        return total;
    }
}
