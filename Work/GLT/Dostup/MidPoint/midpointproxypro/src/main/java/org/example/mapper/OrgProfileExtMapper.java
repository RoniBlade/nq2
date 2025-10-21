package org.example.mapper;

import org.example.dto.view.OrgProfileExtDto;
import org.example.entity.view.OrgProfileExtEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrgProfileExtMapper {
    public OrgProfileExtEntity toEntity(OrgProfileExtDto dto);

    public OrgProfileExtDto toDto(OrgProfileExtEntity entity);
}
