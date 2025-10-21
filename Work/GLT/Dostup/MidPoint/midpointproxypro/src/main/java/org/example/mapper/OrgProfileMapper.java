package org.example.mapper;

import org.example.dto.view.OrgProfileDto;
import org.example.entity.view.OrgProfileEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Base64;

@Mapper(componentModel = "spring")
public interface OrgProfileMapper {

    @Mapping(target = "nameNorm", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getNameNorm(), language))")
    @Mapping(target = "photo", expression = "java(encodePhoto(entity.getPhoto()))")
    OrgProfileDto toDto(OrgProfileEntity entity, @Context String language);

    @Mapping(target = "photo", expression = "java(decodePhoto(dto.getPhoto()))")
    OrgProfileEntity toEntity(OrgProfileDto dto);

    default String encodePhoto(byte[] photo) {
        return (photo != null && photo.length > 0)
                ? Base64.getEncoder().encodeToString(photo)
                : null;
    }

    default byte[] decodePhoto(String photo) {
        return (photo != null && !photo.isEmpty())
                ? Base64.getDecoder().decode(photo)
                : null;
    }
}
