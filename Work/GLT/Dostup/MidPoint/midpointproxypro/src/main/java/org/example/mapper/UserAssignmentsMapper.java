package org.example.mapper;

import org.example.dto.view.UserAssignmentsDto;
import org.example.entity.view.UserAssignmentsEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserAssignmentsMapper {
    UserAssignmentsDto toDto(UserAssignmentsEntity entity);
    UserAssignmentsEntity toEntity(UserAssignmentsDto dto);
}
