package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.RoleProfileExtDto;
import org.example.entity.view.RoleProfileExtEntity;
import org.example.mapper.RoleProfileExtMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.RoleProfileExtRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleProfileExtService {

    private final RoleProfileExtRepository repository;
    private final RoleProfileExtMapper mapper;

    public Page<RoleProfileExtDto> getRoleProfileExt(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDto);
    }

    public Page<RoleProfileExtDto> searchRoleProfileExt(FilterRequest request,
            Pageable pageable) {
        Specification<RoleProfileExtEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), RoleProfileExtEntity.class, null);

        return repository.findAll(spec, pageable)
                .map(mapper::toDto)
                .map(dto -> DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields()));
    }
}
