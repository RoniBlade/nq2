package org.example.service.view;

import lombok.RequiredArgsConstructor;
import org.example.dto.view.UserProfileDto;
import org.example.entity.view.UserProfileEntity;
import org.example.mapper.UserProfileMapper;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.repository.jdbc.UserProfileJdbcRepository;
import org.example.repository.hibernate.view.UserProfileRepository;
import org.example.util.UserProfileColumnMapper;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;
    private final UserProfileJdbcRepository userProfileJdbcRepository;

    public Page<UserProfileDto> getUserProfile(Pageable pageable, String lang) {
        Pageable mappedPageable = UserProfileColumnMapper.mapSortFromSqlColumn(pageable);
        return userProfileJdbcRepository.findAll(pageable, List.of(new FilterNode()));
    }

    public Page<UserProfileDto> searchUserProfile(FilterRequest request, Pageable pageable, String language) {
        Specification<UserProfileEntity> spec = FilterSpecificationBuilder.build(
                null, request.getFilters(), UserProfileEntity.class, null);

        Pageable mappedPageable = UserProfileColumnMapper.mapSortFromSqlColumn(pageable);
        return userProfileJdbcRepository.findAll(pageable, request.getFilters())
                .map(dto -> {
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });

    }
}
