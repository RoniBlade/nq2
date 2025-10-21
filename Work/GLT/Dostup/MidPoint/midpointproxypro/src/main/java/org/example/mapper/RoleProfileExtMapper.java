package org.example.mapper;

import org.example.dto.view.RoleProfileExtDto;
import org.example.entity.view.RoleProfileExtEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleProfileExtMapper {

    RoleProfileExtDto toDto(RoleProfileExtEntity entity);

    RoleProfileExtEntity toEntity(RoleProfileExtDto dto);
}
