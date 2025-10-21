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
@Table(name = "m_access_cert_definition")
public class AccessCertDefinitionEntity {
    @Id
    @Column(name = "oid")
    private UUID oid;

    @Column(name = "nameorig")
    private String nameOrig;

    @Column(name = "namenorm")
    private String nameNorm;

    @Column(name = "fullobject")
    private byte[] fullObject;

    @Column(name = "tenantreftargetoid")
    private UUID tenantRefTargetOid;

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
    private Integer[] policySituations;

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

    @Column(name = "creatorrefrelationid")
    private Integer creatorRefRelationId;

    @Column(name = "createchannelid")
    private Integer createChannelId;

    @Column(name = "createtimestamp")
    private LocalDateTime createTimestamp;

    @Column(name = "modifierreftargetoid")
    private UUID modifierRefTargetOid;

    @Column(name = "modifierreftargettype")
    private String modifierRefTargetType;

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

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "handleruriid")
    private Integer handlerUriId;

    @Column(name = "lastcampaignstartedtimestamp")
    private LocalDateTime lastCampaignStartedTimestamp;

    @Column(name = "lastcampaignclosedtimestamp")
    private LocalDateTime lastCampaignClosedTimestamp;

    @Column(name = "ownerreftargetoid")
    private UUID ownerRefTargetOid;

    @Column(name = "ownerreftargettype")
    private String ownerRefTargetType;

    @Column(name = "ownerrefrelationid")
    private Integer ownerRefRelationId;
}
