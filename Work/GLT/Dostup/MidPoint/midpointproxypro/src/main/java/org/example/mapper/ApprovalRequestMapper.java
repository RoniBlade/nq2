package org.example.mapper;

import org.example.dto.view.ApprovalRequestDto;
import org.example.entity.view.ApprovalRequestEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApprovalRequestMapper {
    @Mapping(target = "objectDisplayName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectDisplayName(), language))")
    @Mapping(target = "objectName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectName(), language))")
    ApprovalRequestDto toDto(ApprovalRequestEntity entity, @Context String language);
    ApprovalRequestEntity toEntity(ApprovalRequestDto dto);
}
