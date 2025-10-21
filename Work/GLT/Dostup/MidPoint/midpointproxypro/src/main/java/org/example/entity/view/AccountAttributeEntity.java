package org.example.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "glt_get_accountattributes_v")
public class AccountAttributeEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "attr_name")
    private String attrName;

    @Column(name = "attr_value")
    private String attrValue;

    @Column(name = "entitlements")
    private Boolean entitlements;
}
