package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RoleProfileDto {

    private Long id;
    private UUID oid;
    private String nameOrig;
    private String nameNorm;
    private String fullObject;
    private String description;
    private String documentation;
    private String indestructible;
    private Integer tenantRefTargetOid;
    private String tenantRefTargetType;
    private Integer tenantRefRelationId;
    private String lifeCycleState;
    private Integer cidSeq;
    private Integer version;
    private Integer policySituations;
    private Integer subTypes;
    private String fullTextInfo;
    private String ext;
    private UUID creatorRefTargetOid;
    private String creatorRefTargetType;
    private Integer creatorRefRelationId;
    private Integer createChannelId;
    private LocalDateTime createTimestamp;
    private UUID modifierRefTargetOid;
    private String modifierRefTargetType;
    private Integer modifierRefRelationId;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimestamp;
    private LocalDateTime dbCreated;
    private LocalDateTime dbModified;
    private String objectType;
    private String costCenter;
    private byte[] photo;
    private String emailAddress; //
    private String locale;
    private String localityOrig;
    private String localityNorm;
    private String preferredLanguage;
    private String telephoneNumber;
    private String timezone;
    private LocalDateTime passwordCreateTimestamp;
    private LocalDateTime passwordModifyTimestamp;
    private String administrativeStatus;
    private String effectiveStatus;
    private LocalDateTime enableTimestamp;
    private LocalDateTime disableTimestamp;
    private String disableReason;
    private String validityStatus;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime validityChangeTimestamp;
    private LocalDateTime archiveTimestamp;
    private Boolean lockoutStatus;
    private LocalDateTime lockoutExpirationTimestamp;
    private byte[] normalizedData;
    private Boolean autoAssignEnabled;
    private String displayNameOrig;
    private String displayNameNorm;
    private String identifier;
    private Boolean requestable;
    private String riskLevel;
    private Integer subType;
    private Integer tenantRefTargetRelationId;
    private UUID targetArchetypeOid;
    private String archetype_icon_class;
    private String archetype_icon_color;
    @JsonProperty("vDisplayName") private String vDisplayName;
}
