package org.example.mapper;

import org.example.dto.view.CaseInfoDto;
import org.example.entity.view.CaseInfoEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseInfoMapper {
    @Mapping(target = "stageApproverDisplayName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getStageApproverDisplayName(), language))")
    CaseInfoDto toDto(CaseInfoEntity entity, @Context String language);
    CaseInfoEntity toEntity(CaseInfoDto dto);
}
