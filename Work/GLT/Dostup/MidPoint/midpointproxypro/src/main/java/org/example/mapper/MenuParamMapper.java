package org.example.mapper;

import org.example.dto.view.MenuParamDto;
import org.example.entity.view.MenuParamEntity;
import org.example.v1.entity.DMenuParamEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuParamMapper {
    DMenuParamEntity toEntity(MenuParamDto dto);
    MenuParamDto toDto(DMenuParamEntity entity);
}
