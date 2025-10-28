package org.example.mapper;

import org.example.dto.view.ObjectInfoLiteDto;
import org.example.entity.view.ObjectInfoLiteEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Base64;

import static org.mapstruct.ReportingPolicy.IGNORE;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ObjectInfoLiteMapper {

    @Mapping(target = "objectName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectName(), language))")
    @Mapping(target = "objectFullName", expression = "java(org.example.util.field.DisplayNameUtil.parseDisplayName(entity.getObjectFullName(), language))")
    @Mapping(target = "photo", expression = "java(encodePhoto(entity.getPhoto()))")
    ObjectInfoLiteDto toDto(ObjectInfoLiteEntity entity, @Context String language);

    @Mapping(target = "photo", expression = "java(decodePhoto(dto.getPhoto()))")
    ObjectInfoLiteEntity toEntity(ObjectInfoLiteDto dto);

    default String encodePhoto(byte[] photo) {
        return (photo != null && photo.length > 0)
                ? Base64.getEncoder().encodeToString(photo)
                : null;
    }

    default byte[] decodePhoto(String base64) {
        return (base64 != null && !base64.isEmpty())
                ? Base64.getDecoder().decode(base64)
                : null;
    }
}
