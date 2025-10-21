package org.example.mapper;

import org.example.dto.CaseDto;
import org.example.entity.CaseEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CaseMapper {

    CaseDto toDto(CaseEntity entity);

    CaseEntity toEntity(CaseDto dto);
}
