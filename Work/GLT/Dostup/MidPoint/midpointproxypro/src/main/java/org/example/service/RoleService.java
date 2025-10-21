package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.RoleDto;
import org.example.entity.RoleEntity;
import org.example.mapper.RoleMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.RoleRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper mapper;
    private final RoleRepository repository;

    public Page<RoleDto> getRole(Pageable pageable) {
        return repository.findAll(pageable)
                .map(entity -> mapper.toDto(entity));
    }

    public Page<RoleDto> searchRole(FilterRequest request, Pageable pageable) {
        Specification<RoleEntity> specification =
                FilterSpecificationBuilder.build(null, request.getFilters(), RoleEntity.class, null);

        return repository.findAll(specification, pageable)
                .map(entity -> {
                    RoleDto dto = mapper.toDto(entity);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }
}
