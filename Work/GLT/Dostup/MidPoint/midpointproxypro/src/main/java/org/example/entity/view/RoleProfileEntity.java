package org.example.entity.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "glt_get_role_profile")
public class RoleProfileEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "nameorig")
    private String nameOrig;

    @Column(name = "namenorm")
    private String nameNorm;

    @Column(name = "fullobject")
    private String fullObject;

    @Column(name = "description")
    private String description;

    @Column(name = "documentation")
    private String documentation;

    @Column(name = "indestructible")
    private String indestructible;

    @Column(name = "tenantreftargetoid")
    private Integer tenantRefTargetOid;

    @Column(name = "tenantreftargettype")
    private String tenantRefTargetType;

    @Column(name = "tenantrefrelationid")
    private Integer tenantRefRelationId;

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

    @Column(name="creatorreftargetoid")
    private UUID creatorRefTargetOid;

    @Column(name="creatorreftargettype")
    private String creatorRefTargetType;

    @Column(name="creatorrefrelationid")
    private Integer creatorRefRelationId;

    @Column(name="createchannelid")
    private Integer createChannelId;

    @Column(name="createtimestamp")
    private LocalDateTime createTimestamp;

    @Column(name="modifierreftargetoid")
    private UUID modifierRefTargetOid;

    @Column(name="modifierreftargettype")
    private String modifierRefTargetType;

    @Column(name="modifierrefrelationid")
    private Integer modifierRefRelationId;

    @Column(name="modifychannelid")
    private Integer modifyChannelId;

    @Column(name="modifytimestamp")
    private LocalDateTime modifyTimestamp;

    @Column(name="dbcreated")
    private LocalDateTime dbCreated;

    @Column(name="dbmodified")
    private LocalDateTime dbModified;

    @Column(name="objecttype")
    private String objectType;

    @Column(name="costcenter")
    private String costCenter;

    @Column(name="photo") // байтовый массив
    private byte[] photo;

    @Column(name = "emailaddress")
    private String emailAddress;

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
    private String timezone;

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

    @Column(name="validitychangetimestamp")
    private LocalDateTime validityChangeTimestamp;

    @Column(name="archivetimestamp")
    private LocalDateTime archiveTimestamp;

    @Column(name="lockoutstatus")
    private Boolean lockoutStatus;

    @Column(name="lockoutexpirationtimestamp")
    private LocalDateTime lockoutExpirationTimestamp;

    @Column(name="normalizeddata") // байтовый массив
    private byte[] normalizedData;

    @Column(name="autoassignenabled")
    private Boolean autoAssignEnabled;

    @Column(name="displaynameorig")
    private String displayNameOrig;

    @Column(name="displaynamenorm")
    private String displayNameNorm;

    @Column(name="identifier")
    private String identifier;

    @Column(name="requestable")
    private Boolean requestable;

    @Column(name="risklevel")
    private String riskLevel;

    @Column(name="subtype")
    private Integer subType;

    @Column(name="tenantreftargetrelationid")
    private Integer tenantRefTargetRelationId;

    @Column(name="targetarchetypeoid")
    private UUID targetArchetypeOid;

    @Column(name="archetype_icon_class")
    private String archetype_icon_class;

    @Column(name="archetype_icon_color")
    private String archetype_icon_color;

    @Column(name="vdisplayname")
    private String vDisplayName;
}
