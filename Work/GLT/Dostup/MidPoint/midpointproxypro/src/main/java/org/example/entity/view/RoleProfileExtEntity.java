package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "glt_get_role_profile_ext")
public class RoleProfileExtEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "ext_attr_value")
    private String ext_attr_value;

    @Column(name = "ext_attr_name")
    private String ext_attr_name;

    @Column(name = "cardinality")
    private String cardinality;

    @Column(name = "type")
    private String type;

    @Column(name = "contanertype")
    private String contanerType;
}
