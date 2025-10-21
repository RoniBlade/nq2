package org.example.service.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.example.dto.view.AssignRoleInfoDto;
import org.example.dto.view.UserPersonasDto;
import org.example.entity.view.UserPersonasEntity;
import org.example.mapper.UserPersonasMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.UserPersonasRepository;
import org.example.repository.jdbc.UserPersonasJdbcRepository;
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
public class UserPersonasService {

    private final UserPersonasRepository repository;
    private final UserPersonasMapper mapper;

    private final UserPersonasJdbcRepository jdbcRepository;

//    public Page<UserPersonasDto> getUserPersonas(Pageable pageable) {
//        return repository.findAll(pageable)
//                .map(mapper::toDto);
//    }
//
//    public Page<UserPersonasDto> searchUserPersonas(FilterRequest request, Pageable pageable) {
//        Specification<UserPersonasEntity> spec = FilterSpecificationBuilder.build(
//                null, request.getFilters(), UserPersonasEntity.class, null);
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    UserPersonasDto dto = mapper.toDto(entity);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }

    public Page<UserPersonasDto> getUserPersonasViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row));
    }

    public Page<UserPersonasDto> searchUserPersonasViaJdbc(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row), request.getFields(), request.getExcludeFields()));
    }
    private UserPersonasDto toDto(Map<String, Object> row) {
        var n = normalizeKeys(row);

        UserPersonasDto dto = new UserPersonasDto();

        dto.setOwnerOid(asUuid(get(n, "ownerOid", "owneroid", "owner_oid")));
        dto.setTargetOid(asUuid(get(n, "targetoid", "targetOid", "target_oid")));
        dto.setOwn_nameOrig(asString(get(n, "own_nameOrig", "own_nameorig", "ownNameOrig", "ownnameorig")));
        dto.setOwn_nameNorm(asString(get(n, "own_nameNorm", "own_namenorm")));
        dto.setOwn_fullName(asString(get(n, "own_fullName", "own_fullname")));
        dto.setPer_nameOrig(asString(get(n, "per_nameOrig", "per_nameorig")));
        dto.setPer_nameNorm(asString(get(n, "per_nameNorm", "per_namenorm")));
        dto.setPer_emailAddress(asString(get(n, "per_emailAddress", "per_emailaddress")));
        dto.setPer_personalNumber(asString(get(n, "per_personalNumber", "per_personalnumber")));
        dto.setVDisplayName(asString(get(n, "vDisplayName", "vdisplayname")));
        dto.setDescription(asString(get(n, "description")));
        dto.setTarget_archetype_oid(asUuid(get(n, "target_archetype_oid")));
        dto.setTarget_archetype_nameOrig(asString(get(n, "target_archetype_nameOrig", "target_archetype_nameorig")));
        dto.setArchetype_icon_class(asString(get(n, "archetype_icon_class")));
        dto.setArchetype_icon_color(asString(get(n, "archetype_icon_color")));

        return dto;
    }
}
