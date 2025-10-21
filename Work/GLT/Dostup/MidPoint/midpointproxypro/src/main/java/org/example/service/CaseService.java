package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.CaseDto;
import org.example.dto.view.CaseInfoDto;
import org.example.entity.CaseEntity;
import org.example.entity.view.CaseInfoEntity;
import org.example.mapper.CaseInfoMapper;
import org.example.mapper.CaseMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.CaseRepository;
import org.example.repository.hibernate.view.CaseInfoRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseInfoRepository repository;
    private final CaseInfoMapper mapper;
    private final CaseRepository caseRepository;
    private final CaseMapper caseMapper;

    public Page<CaseInfoDto> getCaseInfo(Pageable pageable, String language) {
        return repository.findAll(pageable)
                .map(entity -> mapper.toDto(entity, language));
    }

    public Page<CaseInfoDto> searchCaseInfo(FilterRequest request, Pageable pageable, String language) {
        Specification<CaseInfoEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), CaseInfoEntity.class, null
        );
        return repository.findAll(spec, pageable)
                .map(entity -> {
                    CaseInfoDto dto = mapper.toDto(entity, language);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }

    public Page<CaseDto> searchCases(FilterRequest request, Pageable pageable) {
        Specification<CaseEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), CaseEntity.class, null);
        return caseRepository.findAll(spec, pageable)
                .map(entity -> {
                    CaseDto dto = caseMapper.toDto(entity);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }

    public Page<CaseDto> getCaseByOid(Pageable pageable, UUID oid) {
        return caseRepository.getCaseByOid(pageable, oid)
                .map(entity -> caseMapper.toDto(entity));
    }

    public Page<CaseDto> getCaseByName(Pageable pageable, String nameOrig){
        return caseRepository.getCaseByName(pageable, nameOrig)
                .map(entity -> caseMapper.toDto(entity));
    }

}
