package org.example.mapper;

import org.example.dto.view.UserAssignmentsGrafDto;
import org.example.entity.view.UserAssignmentsGrafEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserAssignmentsGrafMapper {

    UserAssignmentsGrafEntity toEntity(UserAssignmentsGrafDto dto);

    @Mapping(target = "targetNameOrig", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getTargetNameOrig(), language))")
    UserAssignmentsGrafDto toDto(UserAssignmentsGrafEntity entity,  @Context String language);
}
