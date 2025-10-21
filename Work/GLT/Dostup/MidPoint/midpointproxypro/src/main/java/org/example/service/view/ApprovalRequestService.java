package org.example.service.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.view.ApprovalRequestDto;
import org.example.mapper.ApprovalRequestMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.ApprovalRequestRepository;
import org.example.repository.jdbc.ApprovalRequestJdbcRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalRequestService {

    private final ApprovalRequestJdbcRepository jdbcRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---------- Публичные методы (JDBC) ----------

    public Page<ApprovalRequestDto> getApprovalRequestViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row, null));
    }

    public Page<ApprovalRequestDto> getApprovalRequestViaJdbc(Pageable pageable, String language) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row, language));
    }

    public Page<ApprovalRequestDto> searchApprovalRequestViaJdbc(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row, null), request.getFields(), request.getExcludeFields()));
    }

    public Page<ApprovalRequestDto> searchApprovalRequestViaJdbc(FilterRequest request, Pageable pageable, String language) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row, language), request.getFields(), request.getExcludeFields()));
    }

    // ---------- Приватный маппинг одной строки ----------

    private ApprovalRequestDto toDto(Map<String, Object> row, String language) {
        // нормализуем ключи один раз: lower + без '_'
        Map<String, Object> n = normalizeKeys(row);

        ApprovalRequestDto dto = new ApprovalRequestDto();

        dto.setId(asLong(n.get("id")));
        dto.setCaseOid(asUuid(n.get("caseoid")));
        // в SELECT алиас: work_itemid → нормализация даст "workitemid"
        dto.setWorkItemId(asLong(n.get("workitemid")));
        dto.setCaseDateCreate(asLocalDateTime(n.get("casedatecreate")));

        dto.setUserOid(asUuid(n.get("useroid")));
        dto.setUserName(asString(n.get("username")));
        dto.setUserFullName(asString(n.get("userfullname")));
        dto.setUserTitle(asString(n.get("usertitle")));
        dto.setUserOrganization(asString(n.get("userorganization")));
        dto.setUserEmail(asString(n.get("useremail")));
        dto.setUserPhone(asString(n.get("userphone")));

        dto.setRequesterOid(asUuid(n.get("requesteroid")));
        dto.setRequesterName(asString(n.get("requestername")));
        dto.setRequesterFullName(asString(n.get("requesterfullname")));
        dto.setRequesterTitle(asString(n.get("requestertitle")));
        dto.setAsRequesterOrganization(asString(n.get("asrequesterorganization")));
        dto.setRequesterEmail(asString(n.get("requesteremail")));
        dto.setRequesterPhone(asString(n.get("requesterphone")));

        dto.setObjectOid(asUuid(n.get("objectoid")));
        dto.setObjectName(asString(n.get("objectname")));
        dto.setObjectDisplayName(normalizeDisplayName(asString(n.get("objectdisplayname")), language));
        dto.setObjectDescription(asString(n.get("objectdescription")));
        dto.setObjectType(asString(n.get("objecttype")));
        dto.setState(asString(n.get("state")));

        dto.setOidApprover(asUuid(n.get("oidapprover")));
        dto.setNameApprover(asString(n.get("nameapprover")));

        return dto;
    }

    // ---------- Хелперы ----------

    private static Map<String, Object> normalizeKeys(Map<String, Object> src) {
        Map<String, Object> out = new HashMap<>(src.size());
        for (var e : src.entrySet()) out.put(norm(e.getKey()), e.getValue());
        return out;
    }

    private static String norm(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '_') sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
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

    private static LocalDateTime asLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime dt) return dt;
        // ISO с офсетом: 2025-05-29T08:24:44.288+00:00
        try { return OffsetDateTime.parse(v.toString()).toLocalDateTime(); } catch (Exception ignored) {}
        // ISO без офсета: 2025-05-29T11:24:44.288123
        try { return LocalDateTime.parse(v.toString()); } catch (Exception ignored) {}
        return null;
    }

    private static String normalizeDisplayName(String raw, String lang) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.charAt(0) != '{') return s; // уже строка, а не JSON-объект
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
