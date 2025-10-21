package org.example.mapper;

import org.example.dto.ObjectArchetypeFieldDto;
import org.example.entity.ObjectArchetypeFieldEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ObjectArchetypeFieldMapper {
    ObjectArchetypeFieldEntity toEntity(ObjectArchetypeFieldDto objectArchetypeFieldDto);
    ObjectArchetypeFieldDto toDto(ObjectArchetypeFieldEntity objectArchetypeFieldEntity);
    List<ObjectArchetypeFieldDto> toDtoList(List<ObjectArchetypeFieldEntity> entities);

}
