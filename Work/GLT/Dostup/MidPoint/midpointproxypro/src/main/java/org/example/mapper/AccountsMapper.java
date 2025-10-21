package org.example.mapper;

import org.example.dto.view.AccountsDto;
import org.example.entity.view.AccountsEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountsMapper {

    public AccountsEntity toEntity(AccountsDto dto);

    @Mapping(target = "nameOrig", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getNameOrig(), language))")
    @Mapping(target = "nameNorm", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getNameNorm(), language))")
    public AccountsDto toDto(AccountsEntity entity, @Context String language);
}
