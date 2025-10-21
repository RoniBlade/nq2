package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.AccessCertDefinitionDto;
import org.example.entity.AccessCertDefinitionEntity;
import org.example.mapper.AccessCertDefinitionMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.AccessCertDefinitionRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessCertDefinitionService {

    private final AccessCertDefinitionRepository repository;
    private final AccessCertDefinitionMapper mapper;

    public Page<AccessCertDefinitionDto> getDefinitionByOid(Pageable pageable, UUID oid) {
        return repository.getByOid(oid, pageable)
                .map(entity -> mapper.toDto(entity));
    }

    public Page<AccessCertDefinitionDto> getDefinitionCampaign(Pageable pageable) {
        return repository.findAll(pageable)
                .map(entity -> mapper.toDto(entity));
    }

    public Page<AccessCertDefinitionDto> searchCampaign(FilterRequest request, Pageable pageable) {
        Specification<AccessCertDefinitionEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), AccessCertDefinitionEntity.class, null);
        return repository.findAll(spec, pageable)
                .map(entity -> {
                    AccessCertDefinitionDto dto = mapper.toDto(entity);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }
}
