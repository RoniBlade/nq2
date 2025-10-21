package org.example.v1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class V1DelegationService {

    private final String FUNCTION_NAME = "d_get_delegations";
    private final StructureService structureService;

    public Page<Map<String, Object>> getDelegations(FilterRequest request, Pageable pageable) {

        List<FilterNode> filters = (request != null && request.getFilters() != null)
                ? request.getFilters() : Collections.emptyList();
        List<String> include = (request != null) ? request.getFields() : null;
        List<String> exclude = (request != null) ? request.getExcludeFields() : null;

        return structureService.fetch(FUNCTION_NAME, null, null, filters, null, include, exclude, pageable);

    }
}
