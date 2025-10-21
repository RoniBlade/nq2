package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.RoleProfileDto;
import org.example.entity.view.RoleProfileEntity;
import org.example.mapper.RoleProfileMapper;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.repository.jdbc.RoleProfileJdbcRepository;
import org.example.repository.hibernate.view.RoleProfileRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleProfileService {

    private final RoleProfileRepository repository;
    private final RoleProfileMapper mapper;
    private final RoleProfileJdbcRepository roleProfileJdbcRepository;
    public Page<RoleProfileDto> getRoleProfile(Pageable pageable) {
        return roleProfileJdbcRepository.findAll(pageable, List.of(new FilterNode()));
    }

    public Page<RoleProfileDto> searchRoleProfile(FilterRequest request,
            Pageable pageable) {
        Specification<RoleProfileEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), RoleProfileEntity.class, null);

        return roleProfileJdbcRepository.findAll(pageable, request.getFilters())
                .map(dto -> {
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }
}
