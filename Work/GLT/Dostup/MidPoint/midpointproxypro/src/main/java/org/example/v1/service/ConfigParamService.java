package org.example.v1.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.v1.dto.DConfigParamDto;
import org.example.dto.view.ConfigParamDto;
import org.example.v1.entity.DConfigParamEntity;
import org.example.entity.view.ConfigParamEntity;
import org.example.mapper.ConfigParamMapper;
import org.example.v1.mapper.DConfigParamMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.ConfigParamRepository;
import org.example.v1.repository.DConfigParamRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfigParamService {

    private final DConfigParamRepository dConfigParamRepository;

    private final DConfigParamMapper dConfigParamMapper;

    public Page<DConfigParamDto> getConfigParams(Pageable pageable) {
        return dConfigParamRepository.findAll(pageable)
                .map(dConfigParamMapper::toDto);
    }

    public Page<DConfigParamDto> searchConfigParams(FilterRequest request, Pageable pageable) {
        Specification<DConfigParamEntity> spec = FilterSpecificationBuilder
                .build(null, request.getFilters(), DConfigParamEntity.class, null);

        return dConfigParamRepository.findAll(spec, pageable)
                .map(entity -> {
                    DConfigParamDto dto = dConfigParamMapper.toDto(entity);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }

    public DConfigParamDto createConfigParam(DConfigParamDto dto) {
        DConfigParamEntity entity = dConfigParamMapper.toEntity(dto);
        DConfigParamEntity saved = dConfigParamRepository.save(entity);
        return dConfigParamMapper.toDto(saved);
    }

    public DConfigParamDto updateConfigParam(UUID oid, DConfigParamDto dto) {
        DConfigParamEntity existing = dConfigParamRepository.findByOid(oid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ConfigParam с oid=" + oid + " не найден"));

        updateDConfigParamEntityFromDto(existing, dto);

        DConfigParamEntity saved = dConfigParamRepository.save(existing);
        return dConfigParamMapper.toDto(saved);
    }

    private void updateDConfigParamEntityFromDto(DConfigParamEntity existing, DConfigParamDto dto) {
        if (dto.getParentoid() != null) {
            existing.setParentoid(dto.getParentoid());
        }
        if (dto.getConfigparam() != null) {
            existing.setConfigparam(dto.getConfigparam());
        }
        if (dto.getValue() != null) {
            existing.setValue(dto.getValue());
        }
        if (dto.getEnabled() != null) {
            existing.setEnabled(dto.getEnabled());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getFullpath() != null) {
            existing.setFullpath(dto.getFullpath());
        }
        if (dto.getSortorder() != null) {
            existing.setSortorder(dto.getSortorder());
        }
    }

    @Transactional
    public void deleteConfigParam(UUID oid) {
        if (!dConfigParamRepository.existsByOid(oid)) {
            throw new EntityNotFoundException("ConfigParam с oid=" + oid + " не найден");
        }
        dConfigParamRepository.deleteByOid(oid);
    }


}
