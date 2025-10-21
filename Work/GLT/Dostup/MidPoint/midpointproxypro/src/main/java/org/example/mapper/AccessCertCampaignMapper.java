package org.example.mapper;

import org.example.dto.AccessCertCampaignDto;
import org.example.entity.AccessCertCampaignEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccessCertCampaignMapper {

    AccessCertCampaignEntity toEntity(AccessCertCampaignDto dto);

    AccessCertCampaignDto toDto(AccessCertCampaignEntity entity);
}
