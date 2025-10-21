package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "glt_get_user_profile_ext")
@Data
public class UserProfileExtEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "ext_attr_name")
    private String extAttrName;

    @Column(name = "ext_attr_value")
    private String extAttrValue;

    @Column(name = "cardinality")
    private String cardinality;

    @Column(name = "type")
    private String type;

    @Column(name = "contanertype")
    private String contanerType;
}
