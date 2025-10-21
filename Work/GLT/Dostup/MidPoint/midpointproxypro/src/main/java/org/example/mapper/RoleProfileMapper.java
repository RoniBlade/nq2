package org.example.mapper;

import org.example.dto.view.RoleProfileDto;
import org.example.entity.view.RoleProfileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleProfileMapper {

    RoleProfileDto toDto(RoleProfileEntity entity);

    RoleProfileEntity toEntity(RoleProfileDto dto);
}
