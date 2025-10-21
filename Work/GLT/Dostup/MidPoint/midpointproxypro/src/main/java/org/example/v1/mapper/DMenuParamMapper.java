package org.example.v1.mapper;

import org.example.v1.dto.DMenuParamDto;
import org.example.v1.entity.DMenuParamEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DMenuParamMapper {
    DMenuParamEntity toEntity(DMenuParamDto dto);
    DMenuParamDto toDto(DMenuParamEntity entity);
}
