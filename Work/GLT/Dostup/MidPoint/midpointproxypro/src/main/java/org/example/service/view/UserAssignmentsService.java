package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.UserAssignmentsDto;
import org.example.dto.view.UserPersonasDto;
import org.example.entity.view.UserAssignmentsEntity;
import org.example.mapper.UserAssignmentsMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.UserAssignmentsRepository;
import org.example.repository.jdbc.UserAssignmentsJdbcRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.example.util.JdbcRows.*;

@Service
@RequiredArgsConstructor
public class UserAssignmentsService {

//    private final UserAssignmentsRepository repository;
//    private final UserAssignmentsMapper mapper;

    private final UserAssignmentsJdbcRepository jdbcRepository;

//    public Page<UserAssignmentsDto> getUserAssignments(Pageable pageable) {
//        return repository.findAll(pageable)
//                .map(mapper::toDto);
//    }
//
//    public Page<UserAssignmentsDto> searchUserAssignments(Pageable pageable, FilterRequest request) {
//        Specification<UserAssignmentsEntity> spec = FilterSpecificationBuilder.build(
//                null, request.getFilters(), UserAssignmentsEntity.class, null
//        );
//
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    UserAssignmentsDto dto = mapper.toDto(entity);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }

    public Page<UserAssignmentsDto> getUserAssignmentsViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row));
    }

    public Page<UserAssignmentsDto> searchUserAssignmentsViaJdbc(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row), request.getFields(), request.getExcludeFields()));
    }
    private UserAssignmentsDto toDto(Map<String, Object> row) {
        var n = normalizeKeys(row);

        UserAssignmentsDto dto = new UserAssignmentsDto();

        dto.setId(asInteger(get(n, "id")));
        dto.setTab(asString(get(n, "tab")));
        dto.setIsRequest(asBoolean(get(n, "isRequest", "isrequest")));
        dto.setRequestor_oid(asString(get(n, "requestor_oid")));
        dto.setRequestor_name(asString(get(n, "requestor_name")));

        dto.setRequestor_displayName(asString(get(n, "requestor_displayName", "requestor_displayname")));
        dto.setRequestor_personalNumber(asString(get(n, "requestor_personalNumber", "requestor_personalnumber")));
        dto.setOwnerOid(asUuid(get(n, "ownerOid", "owneroid")));
        dto.setOwnerType(asString(get(n, "ownerType", "ownertype")));
        dto.setOwner_nameOrig(asString(get(n, "owner_nameOrig", "owner_nameorig")));

        dto.setOwner_displayName(asString(get(n, "owner_displayName", "owner_displayname")));
        dto.setTargetOid(asUuid(get(n, "targetOid", "targetoid")));
        dto.setTargetType(asString(get(n, "targetType", "targettype")));
        dto.setTarget_nameOrig(asString(get(n, "target_nameOrig", "target_nameorig")));
        dto.setPath_number(asInteger(get(n, "path_number")));

        dto.setPredecessor_oid(asString(get(n, "predecessor_oid")));
        dto.setPredecessor_name(asString(get(n, "predecessor_name")));
        dto.setPredecessor_displayName(asString(get(n, "predecessor_displayName", "predecessor_displayname")));
        dto.setPredecessor_objectType(asString(get(n, "predecessor_objectType", "predecessor_objecttype")));
        dto.setPredecessor_order(asString(get(n, "predecessor_order")));

        dto.setPredecessor_archetype_name(asString(get(n, "predecessor_archetype_name")));
        dto.setPredecessor_archetype_displayName(asString(get(n, "predecessor_archetype_displayName", "predecessor_archetype_displayname")));
        dto.setPredecessor_archetype_plural(asString(get(n, "predecessor_archetype_plural")));
        dto.setPredecessor_archetype_icon_class(asString(get(n, "predecessor_archetype_icon_class")));
        dto.setPredecessor_archetype_icon_color(asString(get(n, "predecessor_archetype_icon_color")));

        dto.setTarget_displayName(asString(get(n, "target_displayName", "target_displayname")));
        dto.setTarget_archetype_oid(asString(get(n, "target_archetype_oid")));
        dto.setTarget_archetype_nameOrig(asString(get(n, "target_archetype_nameOrig", "target_archetype_nameorig")));
        dto.setAssgn_validFrom(asLocalDateTime(get(n, "assgn_validFrom", "assgn_validfrom")));
        dto.setAssgn_validTo(asLocalDateTime(get(n, "assgn_validTo", "assgn_validto")));

        dto.setAssgn_administrativeStatus(asString(get(n, "assgn_administrativeStatus", "assgn_administrativestatus")));
        dto.setAssgn_effectiveStatus(asString(get(n, "assgn_effectiveStatus", "assgn_effectivestatus")));
        dto.setArchetype_displayName(asString(get(n, "archetype_displayName", "archetype_displayname")));
        dto.setArchetype_plural(asString(get(n, "archetype_plural")));
        dto.setArchetype_icon_class(asString(get(n, "archetype_icon_class")));

        dto.setArchetype_icon_color(asString(get(n, "archetype_icon_color")));

        return dto;
    }
}
