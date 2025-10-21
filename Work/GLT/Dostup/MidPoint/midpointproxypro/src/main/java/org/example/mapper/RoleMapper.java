package org.example.mapper;

import org.example.dto.RoleDto;
import org.example.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleEntity toEntity(RoleDto dto);

    RoleDto toDto(RoleEntity entity);
}
