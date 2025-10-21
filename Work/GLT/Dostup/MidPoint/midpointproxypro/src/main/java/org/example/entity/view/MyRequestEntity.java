package org.example.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.model.ObjectTypeEnum;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "glt_my_request_v")
public class MyRequestEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "targetoid")
    private UUID targetOid;

    @Column(name = "caseoid")
    private UUID caseOid;

    @Column(name = "casedatecreate")
    private LocalDateTime caseDateCreate;

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "objectname")
    private String objectName;

    @Column(name = "objectdisplayname")
    private String objectDisplayName;

    @Column(name = "objectdescription")
    private String objectDescription;

    @Column(name = "requestername")
    private String requesterName;

    @Column(name = "requesterfullname")
    private String requesterFullName;

    @Column(name = "requestertitle")
    private String requesterTitle;

    @Column(name = "asrequesterorganization")
    private String asRequesterOrganization;

    @Column(name = "requesteremail")
    private String requesterEmail;

    @Column(name = "requesterphone")
    private String requesterPhone;

    @Column(name = "closetime")
    private LocalDateTime closeTime;

    @Column(name = "state")
    private String state;

    @Column(name = "targetnamenorm")
    private String targetNameNorm;

    @Column(name = "objectid")
    private String objectId;


}
