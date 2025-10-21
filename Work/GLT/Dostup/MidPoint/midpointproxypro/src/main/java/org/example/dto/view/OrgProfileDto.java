package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.ObjectTypeEnum;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgProfileDto {
    private Long id;
    private String oid;
    private String parentOrgOid;
    private String parentOrgType;
    private String parentOrgRelation;
    private String nameNorm;
    private String description;
    private String documentation;
    private String indestructible;
    private byte[] fullObject;
    private UUID tenantRefTargetOid;
    private String tenantRefTargetType;
    private Integer tenantRefRelationOid;
    private String lifeCycleState;
    private Long cidSeq;
    private Integer version;
    private Integer policySituations;
    private Integer subTypes;
    private String fullTextInfo;
    private String ext;
    private UUID creatorRefTargetOid;
    private ObjectTypeEnum creatorRefTargetType;
    private Integer creatorRefRelationId;
    private Integer createChannelId;
    private LocalDateTime createTimeStamp;
    private UUID modifierRefTargetOid;
    private ObjectTypeEnum modifierRefTargetType;
    private Integer modifierRefRelationId;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimeStamp;
    private LocalDateTime dbCreated;
    private LocalDateTime dbModified;
    private ObjectTypeEnum objectType;
    private String costCenter;
    private String emailAddress;
    private String photo;
    private String locale;
    private String localityOrig;
    private String localityNorm;
    private String preferredLanguage;
    private String telephoneNumber;
    private String timeZone;
    private LocalDateTime passwordCreateTimeStamp;
    private LocalDateTime passwordModifyTimeStamp;
    private String administrativeStatus;
    private String effectiveStatus;
    private LocalDateTime enableTimeStamp;
    private LocalDateTime disableTimeStamp;
    private String disableReason;
    private String validityStatus;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime validityChangeTimeStamp;
    private LocalDateTime archiveTimeStamp;
    private String lockOutStatus;
    private String normalizedData; // TODO JSONB
    private Boolean autoAssignEnabled;
    private String displayNameOrig;
    private String displayNameNorm;
    private String identifier;
    private Boolean requestable;
    private String riskLevel;
    private Integer displayOrder;
    private Boolean tenant;
    private UUID targetArchetypeOid;
    private String targetArchetypeNameOrig;
    private String archetypeIcon;
    private String archetypeColor;
    @JsonProperty("vDisplayName")
    private String vDisplayName;

}
