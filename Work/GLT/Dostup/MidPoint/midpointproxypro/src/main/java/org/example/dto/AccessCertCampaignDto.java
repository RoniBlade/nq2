package org.example.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AccessCertCampaignDto {
    private UUID oid;
    private String nameOrig;
    private String nameNorm;
    private byte[] fullObject;
    private UUID tenantRefTargetOid;
    private String tenantRefTargetType;
    private Integer tenantRefRelationOid;
    private String lifeCycleState;
    private Integer cidSeq;
    private Integer version;
    private Integer policySituations;
    private String[] subtypes; // change
    private String fulltextInfo;
    private String ext; // change type
    private UUID creatorRefTargetOid;
    private String creatorRefTargetType;
    private String creatorRefRelationOid;
    private String createChannelId;
    private LocalDateTime createTimeStamp;
    private UUID modifierRefTargetOid;
    private String modifierRefTargetType;
    private Integer modifierRefRelationOid;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimeStamp;
    private LocalDateTime db_created;
    private LocalDateTime db_modified;
    private String objectType;
    private UUID definitionRefTargetOid;
    private String definitionRefTargetType;
    private Integer definitionRefRelationId;
    private LocalDateTime endTimeStamp;
    private Integer handlerUriId;
    private Integer campaignIteration;
    private UUID ownerRefTargetOid;
    private String ownerRefTargetType;
    private Integer ownerRefRelationId;
    private String stageNumber;
    private LocalDateTime startTimeStamp;
    private String state;
}
