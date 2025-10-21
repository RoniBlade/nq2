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
@Table(name = "glt_get_accounts_v")
public class AccountsEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "oid")
    private UUID oid;

    @Enumerated(EnumType.STRING)
    @Column(name = "objecttype")
    private ObjectTypeEnum objectType;

    @Column(name = "nameorig")
    private String nameOrig;

    @Column(name = "namenorm")
    private String nameNorm;

    @Column(name = "fullobject")
    private String fullObject;// change

    @Column(name = "tenantreftargetoid")
    private UUID tenantRefTargetOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenantreftargettype")
    private ObjectTypeEnum tenantRefTargetType;

    @Column(name = "tenantrefrelationid")
    private UUID tenantRefRelationId;

    @Column(name = "lifecyclestate")
    private String lifeCycleState;

    @Column(name = "cidseq")
    private Integer cidSeq;

    @Column(name = "version")
    private Integer version;

    @Column(name = "policysituations")
    private Integer policySituations;

    @Column(name = "subtypes")
    private String subtypes; // change

    @Column(name = "fulltextinfo")
    private String fullTextInfo;

    @Column(name = "ext")
    private String ext; // jsonb

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
    private LocalDateTime createTimeStamp; // Используем Timestamp для хранения времени

    @Column(name = "modifierreftargetoid")
    private UUID modifierTefTargetOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "modifierreftargettype")
    private ObjectTypeEnum modifierRefTargetType;;

    @Column(name = "modifierrefrelationid")
    private Integer modifierRefRelationId;

    @Column(name = "modifychannelid")
    private Integer modifyChannelId;

    @Column(name = "modifytimestamp")
    private LocalDateTime modifyTimeStamp; // Используем Timestamp для хранения времени

    @Column(name = "db_created")
    private LocalDateTime db_created;

    @Column(name = "db_modified")
    private LocalDateTime db_modified;

    @Column(name = "objectclassid")
    private Integer objectClassId;

    @Column(name = "resourcereftargetoid")
    private UUID resourceRefTargetOid;

    @Column(name = "resourcerefrelationid")
    private Integer resourceRefRelationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resourcereftargettype")
    private ObjectTypeEnum resourceRefTargetType;

    @Column(name = "intent")
    private String intent;

    @Column(name = "tag")
    private String tag;

    @Column(name = "kind")
    private String kind;

    @Column(name = "dead")
    private Boolean dead;

    @Column(name = "exist")
    private Boolean exist;

    @Column(name = "fullsynchronizationtimestamp")
    private LocalDateTime fullSynchronizationTimeStamp;

    @Column(name = "pendingoperationcount")
    private Integer pendingOperationCount;

    @Column(name = "primaryidentifiervalue")
    private String primaryIdentifierValue;

    @Column(name = "synchronizationsituation")
    private String synchronizationSituation;

    @Column(name = "synchronizationtimestamp")
    private LocalDateTime synchronizationTimeStamp;

    @Column(name = "attributes")
    private String attributes; // jsonb

    @Column(name = "correlationstarttimestamp")
    private LocalDateTime correlationStartTimeStamp;

    @Column(name = "correlationendtimestamp")
    private LocalDateTime correlationEndTimeStamp;

    @Column(name = "correlationcaseopentimestamp")
    private LocalDateTime correlationCaseOpenTimeStamp;

    @Column(name = "correlationsituation")
    private String correlationSituation;

    @Column(name = "disablereasonid")
    private Integer disableReasonId;

    @Column(name = "enabletimestamp")
    private LocalDateTime enableTimeStamp;

    @Column(name = "disabletimestamp")
    private LocalDateTime disableTimeStamp;

    @Column(name = "lastlogintimestamp")
    private LocalDateTime lastLoginTimeStamp;

    @Column(name = "v_displayname")
    private String v_displayName;

    @Column(name = "user_oid")
    private UUID user_oid;
}
