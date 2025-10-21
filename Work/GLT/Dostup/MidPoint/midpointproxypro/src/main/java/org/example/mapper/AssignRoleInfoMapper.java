package org.example.mapper;

import org.example.dto.view.AssignRoleInfoDto;
import org.example.entity.view.AssignRoleInfoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssignRoleInfoMapper {
    public AssignRoleInfoEntity toEntity(AssignRoleInfoDto dto);

    public AssignRoleInfoDto toDto(AssignRoleInfoEntity entity);
}
