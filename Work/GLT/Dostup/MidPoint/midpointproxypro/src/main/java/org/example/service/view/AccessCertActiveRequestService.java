package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.StageDto;
import org.example.dto.view.AccessCertActiveRequestDto;
import org.example.dto.view.MyRequestDto;
import org.example.entity.view.AccessCertActiveRequestEntity;
import org.example.mapper.AccessCertActiveRequestMapper;
import org.example.model.ObjectTypeEnum;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.AccessCertActiveRequestRepository;
import org.example.repository.jdbc.AccessCertActiveRequestJdbcRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.example.util.JdbcRows.*;
import static org.example.util.JdbcRows.get;
import static org.hibernate.engine.config.spi.StandardConverters.asInteger;

@Service
@RequiredArgsConstructor
public class AccessCertActiveRequestService {

    private final AccessCertActiveRequestRepository repository;
    private final AccessCertActiveRequestMapper mapper;

    private final AccessCertActiveRequestJdbcRepository jdbcRepository;

//    public Page<AccessCertActiveRequestDto> getAccessCertActiveRequests(Pageable pageable, String language) {
//        return repository.findAll(pageable)
//                .map(entity -> mapper.toDto(entity, language));
//    }
//
//    public Page<AccessCertActiveRequestDto> searchAccessCertActiveRequests(FilterRequest request, Pageable pageable, String language) {
//        Specification<AccessCertActiveRequestEntity> spec = FilterSpecificationBuilder.build(null, request.getFilters(), AccessCertActiveRequestEntity.class, null);
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    AccessCertActiveRequestDto dto = mapper.toDto(entity, language);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }

    public Page<AccessCertActiveRequestDto> getMyRequestViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row, null));
    }

    public Page<AccessCertActiveRequestDto> searchMyRequestViaJdbc(FilterRequest request, Pageable pageable, String language) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row, language), request.getFields(), request.getExcludeFields()));
    }

    private AccessCertActiveRequestDto toDto(Map<String, Object> row, String language) {
        var n = normalizeKeys(row);

        AccessCertActiveRequestDto dto = new AccessCertActiveRequestDto();
        dto.setId(asLong(get(n, "campaignoid", "campaignOid")));
        dto.setCaseOid(asLong(get(n, "caseoid", "caseOid")));
        dto.setWorkItemId(asLong(get(n, "workitemid", "workItemId")));
        dto.setObjectId(asUuid(get(n, "objectid", "objectId")));
        dto.setObjectType(asString(get(n, "objectType", "objecttype")));
        dto.setObjectName(asString(get(n, "objectName", "objectname")));
        dto.setObjectDisplayName(asString(get(n, "objectDisplayName", "objectdisplayname")));
        dto.setObjectDescription(asString(get(n, "objectDescription", "objectdescription")));
        dto.setObjectTitle(asString(get(n, "objectTitle", "objecttitle")));
        dto.setObjectOrg(asString(get(n, "objectOrg", "objectorg")));
        dto.setTargetId(asUuid(get(n, "targetId", "targetid")));
        dto.setTargetType(ObjectTypeEnum.valueOf(asString(get(n, "targetType", "targettype")))); // TODO изменить тип
        dto.setTargetName(asString(get(n, "targetName", "targetname")));
        dto.setTargetDisplayName(asString(get(n, "targetDisplayName", "targetdisplayname")));
        dto.setTargetDescription(asString(get(n, "targetDescription", "targetdescription")));
        dto.setTargetTitle(asString(get(n, "targetTitle", "targettitle")));
        dto.setTargetOrg(asString(get(n, "targetOrg", "targetorg")));
        dto.setOutcome(asString(get(n, "outcome")));
        dto.setStageNumber(asInteger(get(n, "stageNumber", "stagenumber")));
        dto.setCampaignIteration(asInteger(get(n, "campaignIteration", "campaigniteration")));
        // TODO добавить поле для stage
// Для списка stages предполагается парсинг или создание списка StageDto
// dto.setStage(...);

        dto.setApprover(asUuid(get(n, "approver")));
        dto.setCertName(asString(get(n, "certName")));
        return dto;
    }
}
