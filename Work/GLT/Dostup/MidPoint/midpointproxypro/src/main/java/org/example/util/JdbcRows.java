package org.example.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JdbcRows {
    private JdbcRows() {}

    // ---- ключи ----
    public static Map<String, Object> normalizeKeys(Map<String, Object> src) {
        Map<String, Object> out = new HashMap<>(src.size());
        for (var e : src.entrySet()) out.put(norm(e.getKey()), e.getValue());
        return out;
    }

    public static Object get(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(norm(k));
            if (v != null || m.containsKey(norm(k))) return v;
        }
        return null;
    }

    public static String norm(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '_') sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    // ---- типы ----
    public static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public static Integer asInteger(Object v){
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    public static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    public static UUID asUuid(Object v) {
        if (v == null) return null;
        if (v instanceof UUID u) return u;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    public static LocalDateTime asLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime dt) return dt;
        if (v instanceof Timestamp ts) return ts.toLocalDateTime();
        String s = v.toString();
        try { return OffsetDateTime.parse(s).toLocalDateTime(); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(s); } catch (Exception ignored) {}
        return null;
    }

    public static Boolean asBoolean(Object v) {
        if (v == null)
            return null;
        if (v instanceof Boolean b)
            return b;
        try {return Boolean.parseBoolean(v.toString());} catch (Exception e) {return null;}
    }

    // ---- i18n displayName ----
    public static String displayName(String raw, String lang, ObjectMapper mapper) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.charAt(0) != '{') return s;
        try {
            JsonNode n = mapper.readTree(s);
            if (lang != null && n.path("lang").has(lang)) {
                return n.path("lang").path(lang).asText(null);
            }
            String orig = n.path("orig").asText(null);
            return orig != null ? orig : s;
        } catch (Exception e) {
            return raw;
        }
    }
}
