package org.example.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "glt_delegations_v")
public class DelegationEntity {

    @Id
    @Column(name = "dep_oid")
    private String dep_oid;

    @Column(name = "dep_fullname")
    private String dep_fullName;

    @Column(name = "dep_personalnumber")
    private String dep_personalNumber;

    @Column(name = "dep_emailaddress")
    private String dep_emailAddress;

    @Column(name = "del_oid")
    private String del_oid;

    @Column(name = "del_fullname")
    private String del_fullName;

    @Column(name = "del_personalnumber")
    private String del_personalNumber;

    @Column(name = "del_emailaddress")
    private String del_emailAddress;

    @Column(name = "description")
    private String description;

    @Column(name = "effectivestatus")
    private String effectiveStatus;

    @Column(name = "validfrom")
    private LocalDateTime validFrom;

    @Column(name = "validto")
    private LocalDateTime validTo;

    @Column(name = "allowtransitive")
    private Boolean allowTransitive;

    @Column(name = "certificationworkitems")
    private Boolean certificationWorkItems;

    @Column(name = "casemanagementworkitems")
    private Boolean caseManagementWorkItems;

    @Column(name = "delegated_item_oid")
    private String delegated_item_oid;

    @Column(name = "delegated_item_type")
    private String delegated_item_type;

    @Column(name = "delegated_item_relation")
    private String delegated_item_relation;

    @Column(name = "delegated_item_nameorig")
    private String delegated_item_nameOrig;

    @Column(name = "delegated_item_objecttype")
    private String delegated_item_objectType;

    @Column(name = "delegated_item_displayname")
    private String delegated_item_displayName;

    @Column(name = "delegated_item_description")
    private String delegated_item_description;

    @Column(name = "delegated_item_archetype_name")
    private String delegated_item_archetype_name;

    @Column(name = "delegated_item_archetype_displayname")
    private String delegated_item_archetype_displayName;

    @Column(name = "delegated_item_archetype_plural")
    private String delegated_item_archetype_plural;

    @Column(name = "delegated_item_archetype_icon_class")
    private String delegated_item_archetype_icon_class;

    @Column(name = "delegated_item_archetype_icon_color")
    private String delegated_item_archetype_icon_color;
}
