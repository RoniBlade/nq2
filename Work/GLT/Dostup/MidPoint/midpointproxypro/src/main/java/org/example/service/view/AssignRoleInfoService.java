package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.AssignRoleInfoDto;
import org.example.dto.view.MyRequestDto;
import org.example.entity.view.AssignRoleInfoEntity;
import org.example.mapper.AssignRoleInfoMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.AssignRoleInfoRepository;
import org.example.repository.jdbc.AssignRoleInfoJdbcRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import static org.example.util.JdbcRows.*;
import static org.example.util.JdbcRows.get;

@Service
@RequiredArgsConstructor
public class AssignRoleInfoService {

    private final AssignRoleInfoRepository repository;
    private final AssignRoleInfoMapper mapper;

    private final AssignRoleInfoJdbcRepository jdbcRepository;

//    public Page<AssignRoleInfoDto> getAssignRoleInfo(Pageable pageable) {
//        return repository.findAll(pageable)
//                .map(entity -> mapper.toDto(entity));
//    }
//
//    public Page<AssignRoleInfoDto> searchAssignRoleInfo(FilterRequest request, Pageable pageable) {
//        Specification<AssignRoleInfoEntity> spec = FilterSpecificationBuilder.build(
//                null, request.getFilters(), AssignRoleInfoEntity.class, null);
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    AssignRoleInfoDto dto = mapper.toDto(entity);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }

    public Page<AssignRoleInfoDto> getAssignRoleViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row));
    }

    public Page<AssignRoleInfoDto> searchAssignRoleViaJdbc(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row), request.getFields(), request.getExcludeFields()));
    }
    private AssignRoleInfoDto toDto(Map<String, Object> row) {
        var n = normalizeKeys(row);

        AssignRoleInfoDto dto = new AssignRoleInfoDto();

        dto.setOwnerOid(asUuid(get(n, "ownerOid", "owneroid", "owner_oid")));
        dto.setType(asString(get(n, "type")));
        dto.setContainerType(asString(get(n, "containerType", "containertype", "container_type")));
        dto.setTargetOid(asUuid(get(n, "targetOid", "target_oid", "targetoid")));
        dto.setObject_description(asString(get(n, "object_description")));
        dto.setKind(asString(get(n, "kind")));
        dto.setIntent(asString(get(n, "intent")));

        return dto;
    }

    // жалуется на objectType
}

