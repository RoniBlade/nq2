package org.example.v1.mapper;

import org.example.v1.dto.DConfigParamDto;
import org.example.v1.entity.DConfigParamEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DConfigParamMapper {
    DConfigParamDto toDto(DConfigParamEntity entity);
    DConfigParamEntity toEntity(DConfigParamDto dto);
}
