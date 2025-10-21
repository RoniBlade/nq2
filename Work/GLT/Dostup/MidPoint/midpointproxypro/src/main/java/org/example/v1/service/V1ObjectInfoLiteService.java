package org.example.v1.service;

import lombok.RequiredArgsConstructor;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.util.field.DtoFieldTrimmer;
import org.example.v1.repository.JdbcRepository;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class V1ObjectInfoLiteService {

    private final String FUNCTION_NAME = "d_get_object_info_lite";
    private final JdbcRepository jdbcRepo;
    Pageable pageable;

    public Page<Map<String, Object>> getObjectInfoLite(String param, Pageable pageable) {
        List<Map<String, Object>> row = jdbcRepo.getOneFromFunction(FUNCTION_NAME, param);

        List<Map<String, Object>> content = row.stream()
                .map(r -> DtoFieldTrimmer.trimMap(r, null, null))
                .toList();

        return new PageImpl<>(content, pageable, content.size());
    }
}
