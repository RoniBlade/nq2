package org.example.mapper;

import org.example.dto.AccessCertDefinitionDto;
import org.example.entity.AccessCertDefinitionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccessCertDefinitionMapper {

    AccessCertDefinitionEntity toEntity(AccessCertDefinitionDto dto);

    AccessCertDefinitionDto toDto(AccessCertDefinitionEntity entity);
}
