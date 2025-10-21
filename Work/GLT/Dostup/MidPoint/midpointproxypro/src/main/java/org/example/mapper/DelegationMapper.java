package org.example.mapper;

import org.example.dto.view.DelegationDto;
import org.example.entity.view.DelegationEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DelegationMapper {

//    @Mapping(target = "dep_fullName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getDepFullname(), language))")
//    @Mapping(target = "del_fullName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getDelFullname(), language))")
    DelegationDto toDto(DelegationEntity entity, @Context String language);

    DelegationEntity toEntity(DelegationDto dto);
}
