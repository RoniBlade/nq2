package org.example.mapper;

import org.example.dto.view.UserProfileDto;
import org.example.entity.view.UserProfileEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileMapper{

    @Mapping(target = "name", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getName(), language))")
    @Mapping(target = "fullNameOrig", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getFullNameOrig(), language))")
    @Mapping(target = "fullNameNorm", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getFullNameNorm(), language))")
    @Mapping(target = "photo", expression = "java(encodePhoto(entity.getPhoto()))") // ✅ Добавлено
    UserProfileDto toDto(UserProfileEntity entity, @Context String language);

    @Mapping(target = "photo", expression = "java(decodePhoto(dto.getPhoto()))")
    UserProfileEntity toEntity(UserProfileDto dto);

    default String encodePhoto(byte[] photo) {
        return (photo != null && photo.length > 0)
                ? java.util.Base64.getEncoder().encodeToString(photo)
                : null;
    }

    default byte[] decodePhoto(String base64) {
        return (base64 != null && !base64.isEmpty())
                ? java.util.Base64.getDecoder().decode(base64)
                : null;
    }
}
