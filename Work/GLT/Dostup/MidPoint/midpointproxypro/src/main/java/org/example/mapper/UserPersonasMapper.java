package org.example.mapper;

import org.example.dto.view.UserPersonasDto;

import org.example.entity.view.UserPersonasEntity;
import org.example.entity.view.UserProfileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersonasMapper {
    public UserPersonasDto toDto(UserPersonasEntity entity);
    public UserProfileEntity toEntity(UserPersonasDto dto);
}
