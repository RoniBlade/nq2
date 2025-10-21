package org.example.mapper;

import org.example.dto.view.MyRequestDto;
import org.example.entity.view.MyRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Context;

@Mapper(componentModel = "spring")
public interface MyRequestMapper {

    @Mapping(target = "objectDisplayName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectDisplayName(), language))")
    @Mapping(target = "objectName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectName(), language))")
    MyRequestDto toDto(MyRequestEntity entity, @Context String language);

    MyRequestEntity toEntity(MyRequestDto dto);
}
