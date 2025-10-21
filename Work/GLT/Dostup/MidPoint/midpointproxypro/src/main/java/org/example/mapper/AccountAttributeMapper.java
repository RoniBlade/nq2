package org.example.mapper;

import org.example.dto.view.AccountAttributeDto;
import org.example.entity.view.AccountAttributeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountAttributeMapper {
    AccountAttributeDto toDto(AccountAttributeEntity entity);
    AccountAttributeEntity toEntity(AccountAttributeDto dto);
}
