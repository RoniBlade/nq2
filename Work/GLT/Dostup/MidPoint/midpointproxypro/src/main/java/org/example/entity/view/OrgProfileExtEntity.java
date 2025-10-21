package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "glt_get_org_profile_ext")
public class OrgProfileExtEntity {

    @Id
//    @Column(name = "id")
//    private Integer id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "extattrvalue")
    private String extAttrValue;

    @Column(name = "extattrname")
    private String extAttrName;

    @Column(name = "cardinality")
    private String cardinality;

    @Column(name = "type")
    private String type;

    @Column(name = "contanertype")
    private String contanerType;
}
