package org.example.mapper;

import org.example.dto.view.AccessCertActiveRequestDto;
import org.example.entity.view.AccessCertActiveRequestEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccessCertActiveRequestMapper {

    @Mapping(target = "objectDisplayName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectDisplayName(), language))")
    @Mapping(target = "objectName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectName(), language))")
    @Mapping(target = "targetDisplayName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getTargetDisplayName(), language))")
    @Mapping(target = "stage", expression = "java(org.example.util.field.StageUtil.parseStageList(entity.getStage()))")
    @Mapping(target = "targetDescription", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectName(), language))")
    AccessCertActiveRequestDto toDto(AccessCertActiveRequestEntity entity, @Context String language);

    @Mapping(target = "stage", expression = "java(org.example.util.field.StageUtil.writeStage(dto.getStage()))")
    AccessCertActiveRequestEntity toEntity(AccessCertActiveRequestDto dto);
}
