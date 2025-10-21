package org.example.v1.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

/**
 * ColumnMapRowMapper, который:
 *  1) PGobject json/jsonb -> JsonNode
 *  2) text/varchar, если "похоже на JSON" -> JsonNode
 *  3) одноуровневая распаковка "двойного JSON" (JsonNode-текст, в котором лежит JSON)
 *
 * Можно точнее ограничить авторазбор по именам колонок: jsonTextColumns.
 */
public class JsonAwareColumnMapRowMapper extends ColumnMapRowMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Set<String> jsonTextColumns;

    public JsonAwareColumnMapRowMapper() {
        this.jsonTextColumns = Set.of(
                // примеры:
                "value", "displayvalue", "data", "payload", "meta"
        );
    }

    public JsonAwareColumnMapRowMapper(Set<String> jsonTextColumns) {
        this.jsonTextColumns = jsonTextColumns == null ? Set.of() :
                jsonTextColumns.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        Object val = JdbcUtils.getResultSetValue(rs, index);

        if (val instanceof PGobject pg) {
            String type = safeLower(pg.getType());
            String raw = pg.getValue();
            if (raw == null) return null;

            if ("json".equals(type) || "jsonb".equals(type)) {
                Object node = tryParseJson(raw);
                if (node instanceof JsonNode jn && jn.isTextual()) {
                    Object inner = tryParseJson(jn.textValue());
                    if (inner != null) return inner;
                }
                return node != null ? node : raw;
            }
            // другие пользовательские типы — отдадим строковое представление
            return raw;
        }

        ResultSetMetaData md = rs.getMetaData();
        String typeName = safeLower(md.getColumnTypeName(index));
        if (("json".equals(typeName) || "jsonb".equals(typeName)) && val instanceof String s) {
            Object node = tryParseJson(s);
            if (node != null) return node;
            return s;
        }

        if (val instanceof String s) {
            String colName = safeLower(md.getColumnLabel(index));
            if (looksLikeJson(s) || jsonTextColumns.contains(colName)) {
                Object node = tryParseJson(s);
                if (node != null) return node;
            }
        }

        return val;
    }

    private static String safeLower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.length() < 2) return false;
        char f = t.charAt(0), l = t.charAt(t.length() - 1);
        return (f == '{' && l == '}') || (f == '[' && l == ']');
    }

    private static Object tryParseJson(String raw) {
        try {
            if (raw == null) return null;
            String t = raw.trim();
            if (t.isEmpty()) return null;
            // Можно парсить даже без looksLikeJson — Jackson сам отловит не-JSON
            JsonNode node = MAPPER.readTree(t);
            return node;
        } catch (Exception e) {
            return null; // оставим как строку
        }
    }
}
