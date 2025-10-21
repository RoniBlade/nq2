package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.AccountsDto;
import org.example.dto.view.MyRequestDto;
import org.example.dto.view.RoleProfileDto;
import org.example.entity.view.RoleProfileEntity;
import org.example.mapper.AccountsMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.AccountsRepository;
import org.example.repository.jdbc.AccountsJdbcRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.example.util.JdbcRows.*;
import static org.example.util.JdbcRows.get;

@Service
@RequiredArgsConstructor
public class AccountsService {

    private final AccountsRepository repository;
    private final AccountsMapper mapper;

    private final AccountsJdbcRepository jdbcRepository;
//
//    public Page<AccountsDto> getAccounts(Pageable pageable, String language) {
//        return repository.findAll(pageable)
//                .map(entity -> mapper.toDto(entity, language));
//    }
//
//    public Page<AccountsDto> searchAccounts(FilterRequest request, Pageable pageable, String language) {
//        Specification<AccountsEntity> spec = FilterSpecificationBuilder.build(
//                null, request.getFilters(), AccountsEntity.class, null);
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    AccountsDto dto = mapper.toDto(entity, language);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }

    public Page<Map<String, Object>> getAccounts(Pageable pageable) {
        return jdbcRepository.findAll(pageable);
    }

    public Page<Map<String, Object>> searchAccounts(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable);
//                .map(row -> DtoFieldTrimmer.trim(toDto(row, language), request.getFields(), request.getExcludeFields()));
    }

//    private AccountsDto toDto(Map<String, Object> row, String language) {
//        var n = normalizeKeys(row);
//
//        AccountsDto dto = new AccountsDto();
//        dto.setId(asLong(get(n, "id")));
//        dto.setCaseOid(asUuid(get(n, "caseOid", "case_oid", "caseoid")));
//        dto.setCaseDateCreate(asLocalDateTime(get(n, "caseDateCreate", "case_date_create", "casedatecreate")));
//
//        dto.setObjectType(asString(get(n, "objectType", "object_type", "objecttype")));
//        dto.setObjectName(asString(get(n, "objectName", "object_name", "objectname")));
//        dto.setObjectDisplayName(displayName(asString(get(n, "objectDisplayName", "object_display_name", "objectdisplayname")), language, MAPPER));
//
//        dto.setRequesterName(asString(get(n, "requesterName", "requester_name", "requestername")));
//        dto.setRequesterFullName(asString(get(n, "requesterFullName", "requester_full_name", "requesterfullname")));
//        dto.setRequesterTitle(asString(get(n, "requesterTitle", "requester_title", "requestertitle")));
//        dto.setAsRequesterOrganization(asString(get(n, "asRequesterOrganization", "as_requester_organization", "asrequesterorganization")));
//        dto.setRequesterEmail(asString(get(n, "requesterEmail", "requester_email", "requesteremail")));
//        dto.setRequesterPhone(asString(get(n, "requesterPhone", "requester_phone", "requesterphone")));
//
//        dto.setCloseTime(asLocalDateTime(get(n, "closeTime", "close_time", "closetime")));
//        dto.setState(asString(get(n, "state")));
//
//        dto.setTargetOid(asUuid(get(n, "targetOid", "target_oid", "targetoid")));
//        dto.setTargetNameNorm(asString(get(n, "targetNameNorm", "target_name_norm", "targetnamenorm")));
//        dto.setObjectId(asString(get(n, "objectId", "object_id", "objectid")));
//
//        return dto;
//    }
}
