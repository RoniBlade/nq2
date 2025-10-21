package org.example.dto.view;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.example.model.ObjectTypeEnum;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AccountsDto {

    private Long id;
    private UUID oid;
    private ObjectTypeEnum objectType;
    private String nameOrig;
    private String nameNorm;
    private String fullObject;
    private UUID tenantRefTargetOid;
    private ObjectTypeEnum tenantRefTargetType;
    private UUID tenantRefRelationId;
    private String lifeCycleState;
    private Integer cidSeq;
    private Integer version;
    private Integer policySituations;
    private String subtypes; // change
    private String fullTextInfo;
    private String ext; // jsonb
    private UUID creatorRefTargetOid;
    private ObjectTypeEnum creatorRefTargetType;
    private Integer creatorRefRelationId;
    private Integer createChannelId;
    private LocalDateTime createTimeStamp;
    private UUID modifierTefTargetOid;
    private ObjectTypeEnum modifierRefTargetType;;
    private Integer modifierRefRelationId;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimeStamp;
    private LocalDateTime db_created;
    private LocalDateTime db_modified;
    private Integer objectClassId;
    private UUID resourceRefTargetOid;
    private Integer resourceRefRelationId;
    private ObjectTypeEnum resourceRefTargetType;
    private String intent;
    private String tag;
    private String kind;
    private Boolean dead;
    private Boolean exist;
    private LocalDateTime fullSynchronizationTimeStamp;
    private Integer pendingOperationCount;
    private String primaryIdentifierValue;
    private String synchronizationSituation;
    private LocalDateTime synchronizationTimeStamp;
    private String attributes; // jsonb
    private LocalDateTime correlationStartTimeStamp;
    private LocalDateTime correlationEndTimeStamp;
    private LocalDateTime correlationCaseOpenTimeStamp;
    private LocalDateTime correlationCaseCloseTimeStamp;
    private String correlationSituation;
    private Integer disableReasonId;
    private LocalDateTime enableTimeStamp;
    private LocalDateTime disableTimeStamp;
    private LocalDateTime lastLoginTimeStamp;
    private String v_displayName;
    private UUID user_oid;
}
