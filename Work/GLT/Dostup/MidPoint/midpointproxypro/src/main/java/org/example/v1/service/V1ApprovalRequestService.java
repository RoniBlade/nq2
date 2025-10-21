package org.example.v1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.util.field.DtoFieldTrimmer;
import org.example.v1.repository.JdbcRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class V1ApprovalRequestService {

    private final String FUNCTION_NAME = "d_get_request_approval";
    private final StructureService structureService;
    private final JdbcRepository jdbcRepo;

    public Page<Map<String, Object>> getApprovalRequest(FilterRequest request, Pageable pageable) {

        List<FilterNode> filters = (request != null && request.getFilters() != null)
                ? request.getFilters() : Collections.emptyList();
        List<String> include = (request != null) ? request.getFields() : null;
        List<String> exclude = (request != null) ? request.getExcludeFields() : null;

        return structureService.fetch(FUNCTION_NAME, null, null, filters, null, include, exclude, pageable);

    }
}
