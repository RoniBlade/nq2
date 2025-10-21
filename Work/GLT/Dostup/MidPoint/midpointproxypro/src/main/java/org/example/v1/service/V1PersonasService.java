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
public class V1PersonasService {

    private final String FUNCTION_NAME = "d_get_personas"; // TODO DONT WORK
    private final StructureService structureService;

    public Page<Map<String, Object>> getPersonas(FilterRequest request, Pageable pageable, String param) {

        List<FilterNode> filters = (request != null && request.getFilters() != null)
                ? request.getFilters() : Collections.emptyList();
        List<String> include = (request != null) ? request.getFields() : null;
        List<String> exclude = (request != null) ? request.getExcludeFields() : null;

        return structureService.fetch(FUNCTION_NAME, null, param, filters, null, include, exclude, pageable);
    }
}
