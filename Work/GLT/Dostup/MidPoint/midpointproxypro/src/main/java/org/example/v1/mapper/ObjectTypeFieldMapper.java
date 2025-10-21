package org.example.v1.mapper;

import org.example.v1.dto.ObjectTypeFieldDto;
import org.example.v1.entity.ObjectTypeFieldEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ObjectTypeFieldMapper {
    ObjectTypeFieldEntity toEntity(ObjectTypeFieldDto objectTypeFieldDto);
    ObjectTypeFieldDto toDto(ObjectTypeFieldEntity objectTypeFieldEntity);

}
