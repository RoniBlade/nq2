package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.OrgProfileDto;
import org.example.entity.view.OrgProfileEntity;
import org.example.mapper.OrgProfileMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.OrgProfileRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrgProfileService {

    private final OrgProfileRepository repository;
    private final OrgProfileMapper mapper;

    public Page<OrgProfileDto> getOrgProfile(Pageable pageable, String lang) {
        return repository.findAll(pageable)
                .map(entity -> mapper.toDto(entity, lang));
    }

    public Page<OrgProfileDto> searchOrgProfile(FilterRequest request,
                                                Pageable pageable,
                                                String language) {
        Specification<OrgProfileEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), OrgProfileEntity.class, null);

        return repository.findAll(spec, pageable)
                .map(entity -> mapper.toDto(entity, language))
                .map(dto -> DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields()));
    }
}
