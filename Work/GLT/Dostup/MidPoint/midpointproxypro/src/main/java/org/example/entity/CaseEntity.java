package org.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "m_case")
public class CaseEntity {

    @Id
    @Column(name = "oid")
    private UUID oid;

    @Column(name = "nameorig")
    private String nameOrig;

    @Column(name = "namenorm")
    private String nameNorm;

    @Column(name = "fullobject")
    private String fullObject;

    @Column(name = "tenantreftargetoid")
    private UUID tenantRefTargetOid;

    @Column(name = "tenantreftargettype")
    private String tenantRefTargetType;

    @Column(name = "tenantreflrelationid")
    private Integer tenantRefRelationId;

    @Column(name = "lifecyclestate")
    private String lifeCycleState;

    @Column(name = "cidseq")
    private Integer cidSeq;

    @Column(name = "version")
    private String version;

    @Column(name = "policysituations")
    private Integer policySituations;

    @Column(name = "subtypes")
    private String[] subtypes;

    @Column(name = "fulltextinfo")
    private String fullTextInfo;

    @Column(name = "ext")
    private String ext;

    @Column(name = "creatorreftargetoid")
    private UUID creatorRefTargetOid;

    @Column(name = "creatorreftargettype")
    private String creatorRefTargetType;

    @Column(name = "creatorreflrelationid")
    private Integer creatorRefRelationId;

    @Column(name = "createchannelid")
    private Integer createChannelId;

    @Column(name = "createtimestamp")
    private LocalDateTime createTimestamp;

    @Column(name = "modifierreftargetoid")
    private UUID modifierRefTargetOid;

    @Column(name = "modifierreftargettype")
    private String modifierRefTargetType;

    @Column(name = "modifierreflrelationid")
    private Integer modifierRefRelationId;

    @Column(name = "modifychannelid")
    private Integer modifyChannelId;

    @Column(name = "modifytimestamp")
    private LocalDateTime modifyTimestamp; // или java.util.Date

    @Column(name = "dbcreated")
    private LocalDateTime dbCreated;

    @Column(name = "dbmodified")
    private LocalDateTime dbModified;

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "state")
    private String state;

    @Column(name = "closetimestamp")
    private LocalDateTime closeTimestamp;

    @Column(name = "objectreftargetoid")
    private UUID objectRefTargetOid;

    @Column(name = "objectreftargettype")
    private String objectRefTargetType;

    @Column(name = "objectrefrelationid")
    private Integer objectRefRelationId;

    @Column(name = "parentreftargetoid")
    private UUID parentRefTargetOid;

    @Column(name = "parentreftargettype")
    private String parentRefTargetType;

    @Column(name = "parentrefrelationid")
    private Integer parentRefRelationId;

    @Column(name = "requestorreftargetoid")
    private UUID requestorRefTargetOid;

    @Column(name = "requestorreftargettype")
    private String requestorRefTargetType;

    @Column(name=  "requestorrefrelationid" )
    private Integer requestorRefRelationId;

    @Column( name=  "targetreftargetoid" )
    private UUID targetRefTargetOid;

    @Column( name=  "targetreftargettype" )
    private String targetRefTargetType;

    @Column( name=  "targetrefrelationid" )
    private Integer targetRefRelationId;
}
