package org.example.v1.mapper;

import org.example.v1.dto.DMenuParamDto;
import org.example.v1.dto.EnumValueDto;
import org.example.v1.entity.DMenuParamEntity;
import org.example.v1.entity.EnumValueEntity;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")

public interface EnumValueMapper {
    EnumValueEntity toEntity(EnumValueDto dto);
    EnumValueDto toDto(EnumValueEntity entity);
}
