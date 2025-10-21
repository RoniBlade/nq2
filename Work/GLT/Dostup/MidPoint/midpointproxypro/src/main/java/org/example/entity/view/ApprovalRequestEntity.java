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
@Table(name = "glt_approval_request_v")
public class ApprovalRequestEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "caseoid")
    private UUID caseOid;

    @Column(name = "work_itemid")
    private Long workItemId;

    @Column(name = "casedatecreate")
    private LocalDateTime caseDateCreate;

    @Column(name = "useroid")
    private UUID userOid;

    @Column(name = "username")
    private String userName;

    @Column(name = "userfullname")
    private String userFullName;

    @Column(name = "usertitle")
    private String userTitle;

    @Column(name = "userorganization")
    private String userOrganization;

    @Column(name = "useremail")
    private String userEmail;

    @Column(name = "userphone")
    private String userPhone;

    @Column(name = "requesteroid")
    private UUID requesterOid;

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

    @Column(name = "objectoid")
    private UUID objectOid;

    @Column(name = "objectname")
    private String objectName;

    @Column(name = "objectdisplayname")
    private String objectDisplayName;

    @Column(name = "objectdescription")
    private String objectDescription;

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "state")
    private String state;

    @Column(name = "oidapprover")
    private UUID oidApprover;

    @Column(name = "nameapprover")
    private String nameApprover;
}
