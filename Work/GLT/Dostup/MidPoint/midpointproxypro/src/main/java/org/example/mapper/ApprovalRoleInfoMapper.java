package org.example.mapper;

import org.example.dto.view.ApprovalRoleInfoDto;
import org.example.entity.view.ApprovalRoleInfoEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApprovalRoleInfoMapper {
    ApprovalRoleInfoEntity toEntity(ApprovalRoleInfoDto dto);

    @Mapping(target = "objectDisplayName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectDisplayName(), language))")
    @Mapping(target = "objectName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectName(), language))")
    ApprovalRoleInfoDto toDto(ApprovalRoleInfoEntity entity, @Context String language);
}
