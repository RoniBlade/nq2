package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.AccessCertCampaignDto;
import org.example.entity.AccessCertCampaignEntity;
import org.example.mapper.AccessCertCampaignMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.AccessCertCampaignRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessCertCampaignService {

    private final AccessCertCampaignRepository repository;
    private final AccessCertCampaignMapper mapper;


    public Page<AccessCertCampaignDto> getCampaignByOid(Pageable pageable, UUID oid) {
        return repository.getByOid(oid, pageable)
                .map(entity -> mapper.toDto(entity));
    }

    public Page<AccessCertCampaignDto> getCampaign(Pageable pageable) {
        return repository.findAll(pageable)
                .map(entity -> mapper.toDto(entity));
    }

    public Page<AccessCertCampaignDto> searchCampaign(FilterRequest request, Pageable pageable) {
        Specification<AccessCertCampaignEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), AccessCertCampaignEntity.class, null);
        return repository.findAll(spec, pageable)
                .map(entity -> {
                    AccessCertCampaignDto dto = mapper.toDto(entity);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }
}
