package org.example.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.model.ObjectTypeEnum;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "glt_get_org_profile")
public class OrgProfileEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "oid")
    private String oid;

    @Column(name = "parentorgoid")
    private String parentOrgOid;

    @Column(name = "parentorgtype")
    private String parentOrgType;

    @Column(name = "parentorgrelation")
    private String parentOrgRelation;

    @Column(name = "namenorm")
    private String nameNorm;

    @Column(name = "description")
    private String description;

    @Column(name = "documentation")
    private String documentation;

    @Column(name = "indestructible")
    private String indestructible;

    @Column(name = "fullobject")
    private byte[] fullObject;

    @Column(name = "tenantreftargetoid")
    private UUID tenantRefTargetOid;

    @Column(name = "tenantreftargettype")
    private String tenantRefTargetType;

    @Column(name = "tenantrefrelationid")
    private Integer tenantRefRelationOid;

    @Column(name = "lifecyclestate")
    private String lifeCycleState;

    @Column(name = "cidseq")
    private Integer cidSeq;

    @Column(name = "version")
    private Integer version;

    @Column(name = "policysituations")
    private Integer policySituations;

    @Column(name = "subtypes")
    private Integer subTypes;

    @Column(name = "fulltextinfo")
    private String fullTextInfo;

    @Column(name = "ext")
    private String ext;

    @Column(name = "creatorreftargetoid")
    private UUID creatorRefTargetOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "creatorreftargettype")
    private ObjectTypeEnum creatorRefTargetType;

    @Column(name = "creatorrefrelationid")
    private Integer creatorRefRelationId;

    @Column(name = "createchannelid")
    private Integer createChannelId;

    @Column(name = "createtimestamp")
    private LocalDateTime createTimeStamp;

    @Column(name = "modifierreftargetoid")
    private UUID modifierRefTargetOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "modifierreftargettype")
    private ObjectTypeEnum modifierRefTargetType;

    @Column(name = "modifierrefrelationid")
    private Integer modifierRefRelationId;

    @Column(name = "modifychannelid")
    private Integer modifyChannelId;

    @Column(name = "modifytimestamp")
    private LocalDateTime modifyTimeStamp;

    @Column(name = "dbcreated")
    private LocalDateTime dbCreated;

    @Column(name = "dbmodified")
    private LocalDateTime dbModified;

    @Enumerated(EnumType.STRING)
    @Column(name = "objecttype")
    private ObjectTypeEnum objectType;

    @Column(name = "costcenter")
    private String costCenter;

    @Column(name = "emailaddress")
    private String emailAddress;

    @Column(name = "photo")
    private byte[] photo;

    @Column(name = "locale")
    private String locale;

    @Column(name = "localityorig")
    private String localityOrig;

    @Column(name = "localitynorm")
    private String localityNorm;

    @Column(name = "preferredlanguage")
    private String preferredLanguage;

    @Column(name = "telephonenumber")
    private String telephoneNumber;

    @Column(name = "timezone")
    private String timeZone;

    @Column(name = "passwordcreatetimestamp")
    private LocalDateTime passwordCreateTimeStamp;

    @Column(name = "passwordmodifytimestamp")
    private LocalDateTime passwordModifyTimeStamp;

    @Column(name = "administrativestatus")
    private String administrativeStatus;

    @Column(name = "effectivestatus")
    private String effectiveStatus;

    @Column(name = "enabletimestamp")
    private LocalDateTime enableTimeStamp;

    @Column(name = "disabletimestamp")
    private LocalDateTime disableTimeStamp;

    @Column(name = "disablereason")
    private String disableReason;

    @Column(name = "validitystatus")
    private String validityStatus;

    @Column(name = "validfrom")
    private LocalDateTime validFrom;

    @Column(name = "validto")
    private LocalDateTime validTo;

    @Column(name = "validitychangetimestamp")
    private LocalDateTime validityChangeTimeStamp;

    @Column(name = "archivetimestamp")
    private LocalDateTime archiveTimeStamp;

    @Column(name = "lockoutstatus")
    private String lockOutStatus;

    @Column(name = "normalizeddata")
    private String normalizedData; // TODO JSONB

    @Column(name = "autoassignenabled")
    private Boolean autoAssignEnabled;

    @Column(name = "displaynameorig")
    private String displayNameOrig;

    @Column(name = "displaynamenorm")
    private String displayNameNorm;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "requestable")
    private Boolean requestable;

    @Column(name = "risklevel")
    private String riskLevel;

    @Column(name = "displayorder")
    private Integer displayOrder;

    @Column(name = "tenant")
    private Boolean tenant;

    @Column(name = "targetarchetypeoid")
    private String targetArchetypeOid;

    @Column(name = "targetarchetypenameorig")
    private String targetArchetypeNameOrig;

    @Column(name = "archetypeicon")
    private String archetypeIcon;

    @Column(name = "archetypecolor")
    private String archetypeColor;

    @Column(name = "vdisplayname")
    private String vDisplayName;
}
