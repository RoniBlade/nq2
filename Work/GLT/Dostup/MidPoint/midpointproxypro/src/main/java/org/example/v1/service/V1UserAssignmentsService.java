package org.example.v1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.util.field.DtoFieldTrimmer;
import org.example.v1.repository.JdbcRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class V1UserAssignmentsService {

    private final String FUNCTION_NAME = "d_get_assignment";
    private final StructureService structureService;
    private final JdbcRepository jdbcRepo;


    public Page<Map<String, Object>> getUserAssignments(Pageable pageable, String param) {

        List<Map<String, Object>> row = jdbcRepo.getOneFromFunction(FUNCTION_NAME, param);

        List<Map<String, Object>> content = row.stream()
                .map(r -> DtoFieldTrimmer.trimMap(r, null, null))
                .toList();

        return new PageImpl<>(content, pageable, content.size());
    }

}
