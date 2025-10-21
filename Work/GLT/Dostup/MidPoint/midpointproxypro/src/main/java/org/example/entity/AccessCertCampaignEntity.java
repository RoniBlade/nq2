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
@Table(name = "m_access_cert_campaign")
public class AccessCertCampaignEntity {

    @Id
    @Column(name="oid")
    private UUID oid;

    @Column(name="nameorig")
    private String nameOrig;

    @Column(name="namenorm")
    private String nameNorm;

    @Column(name="fullobject")
    private byte[] fullObject; // change type if needed

    @Column(name="tenantreftargetoid")
    private UUID tenantRefTargetOid;

    @Column(name="tenantreftargettype")
    private String tenantRefTargetType;

    @Column(name="tenantrefrelationoid")
    private Integer tenantRefRelationOid;

    @Column(name="lifecycleState")
    private String lifeCycleState;

    @Column(name="cidseq")
    private Integer cidSeq;

    @Column(name="version")
    private Integer version;

    @Column(name="policiesituations")
    private Integer policySituations;

    @Column(name="subtypes")
    private String[] subtypes; // change

    @Column(name="fulltextinfo")
    private String fulltextInfo;

    @Column(name="ext")
    private String ext; // change type if needed

    @Column(name="creatorreftargetoid")
    private UUID creatorRefTargetOid;

    @Column(name="creatorreftargettype")
    private String creatorRefTargetType;

    @Column(name="creatorrefrelationoid")
    private String creatorRefRelationOid;

    @Column(name="createchannelid")
    private String createChannelId;

    @Column(name="createtimestamp")
    private LocalDateTime createTimeStamp;

    @Column(name="modifierreftargetoid")
    private UUID modifierRefTargetOid;

    @Column(name="modifierreftargettype")
    private String modifierRefTargetType;

    @Column(name="modifierrefrelationoid")
    private Integer modifierRefRelationOid;

    @Column(name="modifychannelid")
    private Integer modifyChannelId;

    @Column(name="modifytimestamp")
    private LocalDateTime modifyTimeStamp;

    @Column(name = "db_created")
    private LocalDateTime db_created;

    @Column(name = "db_modified")
    private LocalDateTime db_modified;

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "definitionreftargetoid")
    private UUID definitionRefTargetOid;

    @Column(name = "definitionreftargettype")
    private String definitionRefTargetType;

    @Column(name = "definitionrefrelationid")
    private Integer definitionRefRelationId;

    @Column(name = "endtimestamp")
    private LocalDateTime endTimeStamp;

    @Column(name = "handleruriiid")
    private Integer handlerUriId;

    @Column(name = "campaigniteration")
    private Integer campaignIteration;

    @Column(name = "ownerreftargetoid")
    private UUID ownerRefTargetOid;

    @Column(name = "ownerreftargettype")
    private String ownerRefTargetType;

    @Column(name = "ownerrefrelationid")
    private Integer ownerRefRelationId;

    @Column(name = "stagenumber")
    private String stageNumber;

    @Column(name = "starttimestamp")
    private LocalDateTime startTimeStamp;

    @Column(name = "state")
    private String state;
}
