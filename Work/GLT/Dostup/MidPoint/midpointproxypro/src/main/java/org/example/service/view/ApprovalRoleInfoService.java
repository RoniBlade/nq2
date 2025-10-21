package org.example.service.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.view.ApprovalRoleInfoDto;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.repository.jdbc.ApprovalRoleInfoJdbcRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalRoleInfoService {

    private final ApprovalRoleInfoJdbcRepository jdbcRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** GET /view */
    public Page<ApprovalRoleInfoDto> getApprovalRoleInfo(Pageable pageable, String lang) {
        return jdbcRepository.findAll(pageable)
                .map(dto -> {
                    dto.setObjectDisplayName(normalizeDisplayName(dto.getObjectDisplayName(), lang));
                    return dto;
                });
    }

    /** POST /filtered-view */
    public Page<ApprovalRoleInfoDto> searchApprovalRoleInfo(FilterRequest request, Pageable pageable, String lang) {
        List<FilterNode> filters = request != null ? request.getFilters() : null;

        Page<ApprovalRoleInfoDto> page = jdbcRepository.findByFilters(filters, pageable)
                .map(dto -> {
                    dto.setObjectDisplayName(normalizeDisplayName(dto.getObjectDisplayName(), lang));
                    return dto;
                });

        if (request != null && (request.getFields() != null || request.getExcludeFields() != null)) {
            return page.map(dto -> DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields()));
        }
        return page;
    }
    // без языка
    public Page<ApprovalRoleInfoDto> getApprovalRoleInfoViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable)
                .map(dto -> {
                    dto.setObjectDisplayName(normalizeDisplayName(dto.getObjectDisplayName(), null));
                    return dto;
                });
    }

    // с языком
    public Page<ApprovalRoleInfoDto> getApprovalRoleInfoViaJdbc(Pageable pageable, String language) {
        return jdbcRepository.findAll(pageable)
                .map(dto -> {
                    dto.setObjectDisplayName(normalizeDisplayName(dto.getObjectDisplayName(), language));
                    return dto;
                });
    }

    // поиск без языка
    public Page<ApprovalRoleInfoDto> searchApprovalRoleInfoViaJdbc(FilterRequest request, Pageable pageable) {
        List<FilterNode> filters = request != null ? request.getFilters() : null;
        return jdbcRepository.findByFilters(filters, pageable)
                .map(dto -> {
                    dto.setObjectDisplayName(normalizeDisplayName(dto.getObjectDisplayName(), null));
                    return DtoFieldTrimmer.trim(dto,
                            request != null ? request.getFields() : null,
                            request != null ? request.getExcludeFields() : null);
                });
    }

    // поиск с языком
    public Page<ApprovalRoleInfoDto> searchApprovalRoleInfoViaJdbc(FilterRequest request, Pageable pageable, String language) {
        List<FilterNode> filters = request != null ? request.getFilters() : null;
        return jdbcRepository.findByFilters(filters, pageable)
                .map(dto -> {
                    dto.setObjectDisplayName(normalizeDisplayName(dto.getObjectDisplayName(), language));
                    return DtoFieldTrimmer.trim(dto,
                            request != null ? request.getFields() : null,
                            request != null ? request.getExcludeFields() : null);
                });
    }

    /** {"orig":"...","norm":"...","lang":{"ru":"..."}} -> локализованная строка; если не JSON — вернуть как есть */
    private static String normalizeDisplayName(String raw, String language) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.charAt(0) != '{') return s;
        try {
            JsonNode n = MAPPER.readTree(s);
            if (language != null && !language.isBlank()) {
                String v = n.path("lang").path(language).asText(null);
                if (v != null && !v.isBlank()) return v;
            }
            String orig = n.path("orig").asText(null);
            return (orig != null && !orig.isBlank()) ? orig : s;
        } catch (Exception e) {
            return raw;
        }
    }
}
