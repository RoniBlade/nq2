package org.example.mapper;

import org.example.dto.GroupDto;
import org.example.entity.view.AssociationObjectViewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(target = "oid", source = "entity.oid")
    @Mapping(target = "displayName", source = "entity.displayName")
    @Mapping(target = "group", source = "groupName")
    GroupDto toDto(AssociationObjectViewEntity entity, String groupName);
}