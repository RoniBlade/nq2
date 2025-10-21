package org.example.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CaseDto {
    private UUID oid;
    private String nameOrig;
    private String nameNorm;
    private String fullObject;
    private UUID tenantRefTargetOid;
    private String tenantRefTargetType;
    private Integer tenantRefRelationId;
    private String lifeCycleState;
    private Integer cidSeq;
    private String version;
    private Integer policySituations;
    private String[] subtypes;
    private String fullTextInfo;
    private String ext; // change
    private UUID creatorRefTargetOid;
    private String creatorRefTargetType;
    private Integer creatorRefRelationId;
    private Integer createChannelId;
    private LocalDateTime createTimestamp; // или java.util.Date
    private UUID modifierRefTargetOid;
    private String modifierRefTargetType;
    private Integer modifierRefRelationId;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimestamp; // или java.util.Date
    private LocalDateTime dbCreated; // дата создания в базе
    private LocalDateTime dbModified; // дата изменения в базе
    private String objectType;
    private String state;
    private LocalDateTime closeTimestamp; // дата закрытия
    private UUID objectRefTargetOid;
    private String objectRefTargetType;
    private Integer objectRefRelationId;
    private UUID parentRefTargetOid;
    private String parentRefTargetType;
    private Integer parentRefRelationId;
    private UUID requestorRefTargetOid;
    private String requestorRefTargetType;
    private Integer requestorRefRelationId;
    private UUID targetRefTargetOid;
    private String targetRefTargetType;
    private Integer targetRefRelationId;
}
