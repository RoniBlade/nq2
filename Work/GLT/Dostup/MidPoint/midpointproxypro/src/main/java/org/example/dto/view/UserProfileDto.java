package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;
import org.example.model.ObjectTypeEnum;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

//@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserProfileDto {
    private Long id;
    private UUID oid;

    @JsonProperty("name")
    private String name;

    private String description;
    private String documentation;
    private String indestructible;
    private String fullObject;

    private UUID tenantRefTargetOid;
    private ObjectTypeEnum tenantRefTargetType;
    private Integer tenantRefRelationId;

    private String lifecycleState;
    private Integer cidSeq;
    private Integer version;
    private String policySituations;
    private Integer subtypes;
    private String fullTextInfo;

    private UUID creatorRefTargetOid;
    private ObjectTypeEnum creatorRefTargetType;
    private Integer creatorRefRelationId;
    private Integer createChannelId;
    private LocalDateTime createTimestamp;

    private UUID modifierRefTargetOid;
    private ObjectTypeEnum modifierRefTargetType;
    private Integer modifierRefRelationId;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimestamp;

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
    private String lockoutStatus;
    private LocalDateTime lockoutExpirationTimestamp;

    private String normalizeddata;

    private String additionalNameOrig;
    private String additionalNameNorm;
    private String employeeNumber;

    private String familyNameOrig;
    private String familyNameNorm;

    private String fullNameOrig;
    private String fullNameNorm;

    private String givenNameOrig;
    private String givenNameNorm;

    private String honorificPrefixOrig;
    private String honorificPrefixNorm;

    private String honorificSuffixOrig;
    private String honorificSuffixNorm;

    private String nicknameOrig;
    private String nicknameNorm;

    private String personalNumber;
    private String titleOrig;
    private String titleNorm;

    private String organizations;
    private String organizationUnits;

    private UUID targetArchetypeOid;
    private String targetArchetypeNameOrig;

    private String archetypeIconClass;
    private String archetypeIconColor;

    @JsonProperty("vDisplayName")
    private String vDisplayName;
}