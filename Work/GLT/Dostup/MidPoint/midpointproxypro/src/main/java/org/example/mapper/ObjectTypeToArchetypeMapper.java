package org.example.mapper;

import org.example.dto.ObjectArchetypeFieldDto;
import org.example.v1.dto.ObjectTypeFieldDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ObjectTypeToArchetypeMapper {
    ObjectArchetypeFieldDto toArchetypeDto(ObjectTypeFieldDto source);
}
