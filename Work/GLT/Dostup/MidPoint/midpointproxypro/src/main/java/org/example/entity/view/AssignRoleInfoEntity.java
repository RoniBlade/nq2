package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "glt_get_assign_role_info")
public class AssignRoleInfoEntity {

    @Id
//    @Column(name = "id")
//    private Integer id;

    @Column(name = "owneroid")
    private UUID ownerOid;

    @Column(name = "type")
    private String type;

    @Column(name = "containertype")
    private String containerType;

    @Column(name = "targetoid")
    private UUID targetOid;

    @Column(name = "object_description")
    private String object_description;

    @Column(name = "kind")
    private String kind;

    @Column(name = "intent")
    private String intent;
}
