package org.example.service.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.view.ObjectInfoLiteDto;
import org.example.repository.hibernate.view.ObjectInfoLiteRepository;
import org.example.repository.jdbc.ObjectInfoLiteJdbcRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.model.filter.FilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectInfoLiteService {

    private final ObjectInfoLiteRepository repository;
    private final ObjectInfoLiteJdbcRepository jdbcRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- JDBC → DTO ---

    public Page<ObjectInfoLiteDto> getObjectInfoLiteViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable)
                .map(row -> toDto(row, null));
    }

    public Page<ObjectInfoLiteDto> searchObjectInfoLiteViaJdbc(FilterRequest request,
                                                               Pageable pageable,
                                                               String language) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(
                        toDto(row, language),
                        request.getFields(),
                        request.getExcludeFields()
                ));
    }

    // --- Маппинг одной строки ---

    private ObjectInfoLiteDto toDto(Map<String, Object> m, String language) {
        ObjectInfoLiteDto dto = new ObjectInfoLiteDto();
        dto.setId(asLong(val(m, "id")));
        dto.setOid(asUuid(val(m, "oid")));
        dto.setObjectType(asString(val(m, "objectType", "objecttype", "object_type")));
        dto.setObjectName(asString(val(m, "objectName", "object_name", "objectname")));
        dto.setObjectFullName(normalizeDisplayName(
                asString(val(m, "objectFullName", "object_fullname", "objectfullname")),
                language
        ));
        dto.setObjectDescription(asString(val(m, "objectDescription", "object_description", "objectdescription")));
        dto.setUserTitle(asString(val(m, "userTitle", "user_title")));
        dto.setUserEmailAddress(asString(val(m, "userEmailAddress", "user_emailaddress", "user_email", "useremailaddress")));
        dto.setUserTelephoneNumber(asString(val(m, "userTelephoneNumber", "user_telephonenumber", "user_phone", "usertelephone")));
        dto.setPhoto(asPhoto(val(m, "photo", "user_photo", "userPhoto")));
        return dto;
    }

    // --- Хелперы ключей/типов ---

    private static Object val(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            if (m.containsKey(k)) return m.get(k);
        }
        return null;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private static UUID asUuid(Object v) {
        if (v == null) return null;
        if (v instanceof UUID u) return u;
        try { return UUID.fromString(v.toString()); } catch (Exception e) { return null; }
    }

    private static String asPhoto(Object v) {
        if (v == null) return null;
        if (v instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        return v.toString();
    }

    private static LocalDateTime asLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime dt) return dt;
        if (v instanceof Timestamp ts) return ts.toLocalDateTime();
        try { return LocalDateTime.parse(v.toString()); } catch (Exception e) { return null; }
    }

    private static String normalizeDisplayName(String raw, String lang) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.charAt(0) != '{') return s; // уже плоская строка
        try {
            JsonNode n = MAPPER.readTree(s);
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
