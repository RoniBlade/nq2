package org.example.v1.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.v1.dto.EnumValueDto;
import org.example.v1.entity.EnumValueEntity;
import org.example.v1.mapper.EnumValueMapper;
import org.example.v1.repository.EnumValueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnumValueDefinitionsService {

    private final EnumValueRepository repository;
    private final EnumValueMapper mapper;

    public EnumValueDto create(EnumValueDto dto) {
        EnumValueEntity entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    public EnumValueDto updateEnumValue(UUID oid, EnumValueDto dto) {
        EnumValueEntity existing = repository.findByOid(oid)
                .orElseThrow(() -> new EntityNotFoundException("Запись d_enum_values oid=" + oid + " не найдена"));

        updateEnumValueEntityFromDto(existing, dto);

        return mapper.toDto(repository.save(existing));
    }

    private void updateEnumValueEntityFromDto(EnumValueEntity existing, EnumValueDto dto) {
        if (dto.getEnumtype() != null) {
            existing.setEnumtype(dto.getEnumtype());
        }
        if (dto.getEnumvalue() != null) {
            existing.setEnumvalue(dto.getEnumvalue());
        }
    }

    @Transactional
    public void delete(UUID oid) {
        if (!repository.existsByOid(oid)) {
            throw new EntityNotFoundException("Запись d_enum_values id=" + oid + " не найдена");
        }
        repository.deleteByOid(oid);
    }

    public Page<EnumValueDto> getConfigParams(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public Page<EnumValueDto> searchConfigParams(FilterRequest request, Pageable pageable) {
        Specification<EnumValueEntity> spec = FilterSpecificationBuilder
                .build(null, request.getFilters(), EnumValueEntity.class, null);

        return repository.findAll(spec, pageable)
                .map(entity -> {
                    EnumValueDto dto = mapper.toDto(entity);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }
}
