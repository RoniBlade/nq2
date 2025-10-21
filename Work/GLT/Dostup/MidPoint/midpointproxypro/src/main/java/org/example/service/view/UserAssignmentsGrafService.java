package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.UserAssignmentsGrafDto;
import org.example.entity.view.UserAssignmentsGrafEntity;
import org.example.mapper.UserAssignmentsGrafMapper;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.repository.jdbc.UserAssignmentsGrafJdbcRepository;
import org.example.repository.hibernate.view.UserAssignmentsGrafRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAssignmentsGrafService {

    private final UserAssignmentsGrafMapper mapper;
    private final UserAssignmentsGrafRepository repository;


    private final UserAssignmentsGrafJdbcRepository userAssignmentsGrafJdbcRepository;

    public Page<UserAssignmentsGrafDto> getUserAssignmentsGraf(Pageable pageable, String lang) {
        return userAssignmentsGrafJdbcRepository.findAll(pageable, List.of(new FilterNode()));
    }

    public Page<UserAssignmentsGrafDto> searchUserAssignmentsGraf(FilterRequest request,
                                                                  Pageable pageable,
                                                                  String lang) {
        Specification<UserAssignmentsGrafEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), UserAssignmentsGrafEntity.class, null
        );

        return userAssignmentsGrafJdbcRepository.findAll(pageable, request.getFilters())
                .map(dto -> {
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }
}
