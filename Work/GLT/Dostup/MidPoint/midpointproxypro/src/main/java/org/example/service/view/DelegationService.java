package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.AssignRoleInfoDto;
import org.example.dto.view.DelegationDto;
import org.example.entity.view.DelegationEntity;
import org.example.mapper.DelegationMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.DelegationRepository;
import org.example.repository.jdbc.DelegationsJdbcRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static org.example.util.JdbcRows.*;

@Service
@RequiredArgsConstructor
public class DelegationService {

    private final DelegationRepository repository;
    private final DelegationMapper mapper;

    private final DelegationsJdbcRepository jdbcRepository;


//    public Page<DelegationDto> getDelegations(Pageable pageable, String language) {
//        return repository.findAll(pageable)
//                .map(entity -> mapper.toDto(entity, language));
//    }
//
//    public Page<DelegationDto> searchDelegations(FilterRequest request, Pageable pageable, String language) {
//        Specification<DelegationEntity> spec = FilterSpecificationBuilder.build(
//                null, request.getFilters(), DelegationEntity.class, null);
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    DelegationDto dto = mapper.toDto(entity, language);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }


    public Page<DelegationDto> getDelegationViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row));
    }

    public Page<DelegationDto> searchDelegationViaJdbc(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row), request.getFields(), request.getExcludeFields()));
    }
    private DelegationDto toDto(Map<String, Object> row) {
        var n = normalizeKeys(row);

        DelegationDto dto = new DelegationDto();
        dto.setDep_oid(asString(get(n, "dep_oid")));
        dto.setDep_fullName(asString(get(n, "dep_fullName", "dep_fullname")));
        dto.setDep_personalNumber(asString(get(n, "dep_personalNumber", "dep_personalnumber")));
        dto.setDep_emailAddress(asString(get(n, "dep_emailAddress", "dep_emailaddress")));
        dto.setDel_oid(asString(get(n, "del_oid")));
        dto.setDel_fullName(asString(get(n, "del_fullName", "del_fullname")));
        dto.setDel_personalNumber(asString(get(n, "del_personalNumber", "del_personalnumber")));
        dto.setDel_emailAddress(asString(get(n, "del_emailAddress", "del_emailaddress")));
        dto.setDescription(asString(get(n, "description")));
        dto.setEffectiveStatus(asString(get(n, "effectiveStatus", "effectivestatus")));
        dto.setValidFrom(asLocalDateTime(get(n, "validFrom", "validfrom")));
        dto.setValidTo(asLocalDateTime(get(n, "validTo", "validto")));
        dto.setAllowTransitive(asBoolean(get(n, "allowTransitive", "allowtransitive")));
        dto.setCertificationWorkItems(asBoolean(get(n, "certificationWorkItems", "certificationworkitems")));
        dto.setCaseManagementWorkItems(asBoolean(get(n, "caseManagementWorkItems", "casemanagementworkitems")));
        dto.setDelegated_item_oid(asString(get(n, "delegated_item_oid")));
        dto.setDelegated_item_type(asString(get(n, "delegated_item_type")));
        dto.setDelegated_item_relation(asString(get(n, "delegated_item_relation")));
        dto.setDelegated_item_nameOrig(asString(get(n, "delegated_item_nameOrig")));
        dto.setDelegated_item_objectType(asString(get(n, "delegated_item_objectType", "delegated_item_objecttype")));
        dto.setDelegated_item_displayName(asString(get(n, "delegated_item_displayName", "delegated_item_displayname")));
        dto.setDelegated_item_description(asString(get(n, "delegated_item_description")));
        dto.setDelegated_item_archetype_name(asString(get(n, "delegated_item_archetype_name")));
        dto.setDelegated_item_archetype_displayName(asString(get(n, "delegated_item_archetype_displayName", "delegated_item_archetype_displayname")));
        dto.setDelegated_item_archetype_plural(asString(get(n, "delegated_item_archetype_plural")));
        dto.setDelegated_item_archetype_icon_class(asString(get(n, "delegated_item_archetype_icon_class")));
        dto.setDelegated_item_archetype_icon_color(asString(get(n, "delegated_item_archetype_icon_color")));

        return dto;
    }
}
