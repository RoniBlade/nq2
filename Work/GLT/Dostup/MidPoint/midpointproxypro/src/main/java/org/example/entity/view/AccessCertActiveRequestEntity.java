package org.example.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.model.ObjectTypeEnum;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "glt_access_cert_active_request_v")
public class AccessCertActiveRequestEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "caseoid")
    private Long caseOid;

    @Column(name = "campaignoid")
    private UUID campaignOid;

    @Column(name = "workitemid")
    private Long workItemId;

    @Column(name = "objectid")
    private UUID objectId;

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "objectname")
    private String objectName;

    @Column(name = "objectdisplayname")
    private String objectDisplayName;

    @Column(name = "objectdescription")
    private String objectDescription;

    @Column(name = "objecttitle")
    private String objectTitle;

    @Column(name = "objectorg")
    private String objectOrg;

    @Column(name = "targetid")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "targettype")
    private ObjectTypeEnum targetType;

    @Column(name = "targetname")
    private String targetName;

    @Column(name = "targetdisplayname")
    private String targetDisplayName;

    @Column(name = "targetdescription")
    private String targetDescription;

    @Column(name = "targettitle")
    private String targetTitle;

    @Column(name = "targetorg")
    private String targetOrg;

    @Column(name = "outcome")
    private String outcome;

    @Column(name = "stagenumber")
    private Integer stageNumber;

    @Column(name = "campaigniteration")
    private Integer campaignIteration;

    @Column(name = "stage")
    private String stage;

    @Column(name = "approver")
    private UUID approver;

    @Column(name = "certname")
    private String certName;
}
