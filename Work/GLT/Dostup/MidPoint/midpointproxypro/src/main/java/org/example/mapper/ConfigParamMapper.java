package org.example.mapper;

import org.example.dto.view.ConfigParamDto;
import org.example.entity.view.ConfigParamEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConfigParamMapper {
    ConfigParamDto toDto(ConfigParamEntity entity);
    ConfigParamEntity toEntity(ConfigParamDto dto);
}
