package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.AccountAttributeDto;
import org.example.dto.view.UserPersonasDto;
import org.example.entity.view.AccountAttributeEntity;
import org.example.mapper.AccountAttributeMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.AccountAttributeRepository;
import org.example.repository.jdbc.AccountAttributeJdbcRepository;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.util.field.DtoFieldTrimmer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import static org.example.util.JdbcRows.*;

@Service
@RequiredArgsConstructor
public class AccountAttributeService {

    private final AccountAttributeRepository repository;
    private final AccountAttributeMapper mapper;

    private final AccountAttributeJdbcRepository jdbcRepository;


//    public Page<AccountAttributeDto> getAccountAttributes(Pageable pageable, String language) {
//        Specification<AccountAttributeEntity> spec =
//                FilterSpecificationBuilder.build(null, null, AccountAttributeEntity.class, null);
//        return repository.findAll(spec, pageable)
//                .map(mapper::toDto);
//    }
//
//    public Page<AccountAttributeDto> searchAccountAttributes(FilterRequest request,
//                                                             Pageable pageable,
//                                                             String language) {
//        Specification<AccountAttributeEntity> spec =
//                FilterSpecificationBuilder.build(null, request.getFilters(), AccountAttributeEntity.class, null);
//        return repository.findAll(spec, pageable)
//                .map(entity -> {
//                    AccountAttributeDto dto = mapper.toDto(entity);
//                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
//                });
//    }

    public Page<AccountAttributeDto> getAccountAttributeViaJdbc(Pageable pageable) {
        return jdbcRepository.findAll(pageable).map(row -> toDto(row));
    }

    public Page<AccountAttributeDto> searchAccountAttributeViaJdbc(FilterRequest request, Pageable pageable) {
        return jdbcRepository.findByFilters(request.getFilters(), pageable)
                .map(row -> DtoFieldTrimmer.trim(toDto(row), request.getFields(), request.getExcludeFields()));
    }
    private AccountAttributeDto toDto(Map<String, Object> row) {
        var n = normalizeKeys(row);

        AccountAttributeDto dto = new AccountAttributeDto();

        dto.setOid(asUuid(get(n, "oid")));
        dto.setAttrName(asString(get(n,"attr_name", "attrName", "attrname")));
        dto.setAttrValue(asString(get(n,"attr_value", "attrValue", "attrvalue")));
        dto.setEntitlements(asBoolean(get(n, "entitlements")));

        return dto;
    }
}

