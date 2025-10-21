package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "glt_approval_role_info")
public class ApprovalRoleInfoEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "owneroid")
    private UUID owneroid;

    @Column(name = "containertype")
    private String containerType;

    @Column(name = "targettype")
    private String targetType;

    @Column(name = "objectname")
    private String objectName;

    @Column(name = "objectdisplayname")
    private String objectDisplayName;

    @Column(name = "objectdescription")
    private String objectDescription;
}
