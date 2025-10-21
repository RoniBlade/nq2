package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.DelegationDto;
import org.example.dto.view.OrgProfileExtDto;
import org.example.entity.view.OrgProfileExtEntity;
import org.example.mapper.OrgProfileExtMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.OrgProfileExtRepository;
import org.example.repository.jdbc.OrgProfileExtJdbcRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import static org.example.util.JdbcRows.*;

@Service
@RequiredArgsConstructor
public class OrgProfileExtService {

    private final OrgProfileExtRepository repository;
    private final OrgProfileExtMapper mapper;

    private final OrgProfileExtJdbcRepository jdbcRepository;

//    public Page<OrgProfileExtDto> searchUserProfileExt(FilterRequest request,
//                                                       Pageable pageable) {
//        Specification<OrgProfileExtEntity> spec = FilterSpecificationBuilder.build(
//                null, request.getFilters(), OrgProfileExtEntity.class, null);
//
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    OrgProfileExtDto dto = mapper.toDto(entity);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }
public Page<OrgProfileExtDto> getOrgProfileExtViaJdbc(Pageable pageable) {
    return jdbcRepository.findAll(pageable).map(row -> toDto(row));
}

    public Page<OrgProfileExtDto> searchOrgProfileExtViaJdbc(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row), request.getFields(), request.getExcludeFields()));
    }
    private OrgProfileExtDto toDto(Map<String, Object> row) {
        var n = normalizeKeys(row);

        OrgProfileExtDto dto = new OrgProfileExtDto();

        dto.setOid(asUuid(get(n, "oid")));
        dto.setExtAttrValue(asString(get(n, "extattrvalue", "extAttrValue")));
        dto.setExtAttrName(asString(get(n, "extattrname", "extAttrName")));
        dto.setCardinality(asString(get(n, "cardinality")));
        dto.setType(asString(get(n, "type")));
        dto.setContanerType(asString(get(n, "contanertype", "contanerType")));

        return dto;
    }
}
