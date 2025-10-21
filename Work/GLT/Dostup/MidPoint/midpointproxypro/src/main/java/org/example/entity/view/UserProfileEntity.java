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
@Table(name = "glt_get_user_profile")
public class UserProfileEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "nameorig")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "documentation")
    private String documentation;

    @Column(name = "indestructible")
    private String indestructible;

    @Column(name = "fullobject")
    private String fullObject;

    @Column(name = "tenantreftargetoid")
    private UUID tenantRefTargetOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenantreftargettype")
    private ObjectTypeEnum tenantRefTargetType;

    @Column(name = "tenantrefrelationid")
    private Integer tenantRefRelationId;

    @Column(name = "lifecyclestate")
    private String lifecycleState;

    @Column(name = "cidseq")
    private Integer cidSeq;

    @Column(name = "version")
    private Integer version;

    @Column(name = "policysituations")
    private String policySituations;

    @Column(name = "subtypes")
    private Integer subtypes;

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
    private LocalDateTime createTimestamp;

    @Column(name = "modifierreftargetoid")
    private UUID modifierRefTargetOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "modifierreftargettype")
    private ObjectTypeEnum modifierRefTargetType;;

    @Column(name = "modifierrefrelationid")
    private Integer modifierRefRelationId;

    @Column(name = "modifychannelid")
    private Integer modifyChannelId;

    @Column(name = "modifytimestamp")
    private LocalDateTime modifyTimestamp;

    @Column(name = "db_created")
    private LocalDateTime dbCreated;

    @Column(name = "db_modified")
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
    private LocalDateTime passwordCreateTimestamp;

    @Column(name = "passwordmodifytimestamp")
    private LocalDateTime passwordModifyTimestamp;

    @Column(name = "administrativestatus")
    private String administrativeStatus;

    @Column(name = "effectivestatus")
    private String effectiveStatus;

    @Column(name = "enabletimestamp")
    private LocalDateTime enableTimestamp;

    @Column(name = "disabletimestamp")
    private LocalDateTime disableTimestamp;

    @Column(name = "disablereason")
    private String disableReason;

    @Column(name = "validitystatus")
    private String validityStatus;

    @Column(name = "validfrom")
    private LocalDateTime validFrom;

    @Column(name = "validto")
    private LocalDateTime validTo;

    @Column(name = "validitychangetimestamp")
    private LocalDateTime validityChangeTimestamp;

    @Column(name = "archivetimestamp")
    private LocalDateTime archiveTimestamp;

    @Column(name = "lockoutstatus")
    private String lockoutStatus;

    @Column(name = "lockoutexpirationtimestamp")
    private LocalDateTime lockoutExpirationTimestamp;

    @Column(name = "normalizeddata")
    private String normalizeddata;

    @Column(name = "additionalnameorig")
    private String additionalNameOrig;

    @Column(name = "additionalnamenorm")
    private String additionalNameNorm;

    @Column(name = "employeenumber")
    private String employeeNumber;

    @Column(name = "familynameorig")
    private String familyNameOrig;

    @Column(name = "familynamenorm")
    private String familyNameNorm;

    @Column(name = "fullnameorig")
    private String fullNameOrig;

    @Column(name = "fullnamenorm")
    private String fullNameNorm;

    @Column(name = "givennameorig")
    private String givenNameOrig;

    @Column(name = "givennamenorm")
    private String givenNameNorm;

    @Column(name = "honorificprefixorig")
    private String honorificPrefixOrig;

    @Column(name = "honorificprefixnorm")
    private String honorificPrefixNorm;

    @Column(name = "honorificsuffixorig")
    private String honorificSuffixOrig;

    @Column(name = "honorificsuffixnorm")
    private String honorificSuffixNorm;

    @Column(name = "nicknameorig")
    private String nicknameOrig;

    @Column(name = "nicknamenorm")
    private String nicknameNorm;

    @Column(name = "personalnumber")
    private String personalNumber;

    @Column(name = "titleorig")
    private String titleOrig;

    @Column(name = "titlenorm")
    private String titleNorm;

    @Column(name = "organizations")
    private String organizations;

    @Column(name = "organizationunits")
    private String organizationUnits;

    @Column(name = "targetarchetypeoid")
    private UUID targetArchetypeOid;

    @Column(name = "targetarchetypenameorig")
    private String targetArchetypeNameOrig;

    @Column(name = "archetype_icon_class")
    private String archetypeIconClass;

    @Column(name = "archetype_icon_color")
    private String archetypeIconColor;

    @Column(name = "vdisplayname")
    private String vDisplayName;
}
