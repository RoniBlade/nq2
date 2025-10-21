package org.example.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AccessCertDefinitionDto {

    private UUID oid;
    private String nameOrig;
    private String nameNorm;
    private byte[] fullObject;
    private UUID tenantRefTargetOid;
    private String tenantRefTargetType;
    private Integer tenantRefRelationId;
    private String lifeCycleState;
    private Integer cidSeq; // предполагаемый числовой тип
    private Integer version;
    private Integer[] policySituations;
    private String[] subtypes;
    private String fullTextInfo;
    private String ext;
    private UUID creatorRefTargetOid;
    private String creatorRefTargetType;
    private Integer creatorRefRelationId;
    private Integer createChannelId;
    private LocalDateTime createTimestamp; // дата и время создания
    private UUID modifierRefTargetOid;
    private String modifierRefTargetType;
    private Integer modifierRefRelationId;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimestamp; // дата и время изменения
    private LocalDateTime db_created; // дата создания в базе
    private LocalDateTime db_modified; // дата изменения в базе
    private String objectType;
    private Integer handlerUriId;
    private LocalDateTime lastCampaignStartedTimestamp; // дата начала последней кампании
    private LocalDateTime lastCampaignClosedTimestamp;  // дата окончания последней кампании
    private UUID ownerRefTargetOid;
    private String ownerRefTargetType;
    private Integer ownerRefRelationId;
}
