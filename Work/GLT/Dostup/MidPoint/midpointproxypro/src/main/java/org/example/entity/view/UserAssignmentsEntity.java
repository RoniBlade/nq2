package org.example.entity.view;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "glt_user_assignments_v")
public class UserAssignmentsEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "tab")
    private String tab;

    @Column(name = "isrequest")
    private Boolean isRequest;

    @Column(name = "requestor_oid")
    private String requestor_oid;

    @Column(name = "requestor_name")
    private String requestor_name;

    @Column(name = "requestor_displayname")
    private String requestor_displayName;

    @Column(name = "requestor_personalnumber")
    private String requestor_personalNumber;

    @Column(name = "owneroid")
    private UUID ownerOid;

    @Column(name = "ownertype")
    private String ownerType;

    @Column(name = "owner_nameorig")
    private String owner_nameOrig;

    @Column(name = "owner_displayname")
    private String owner_displayName;

    @Column(name = "targetoid")
    private UUID targetOid;

    @Column(name = "targettype")
    private String targetType;

    @Column(name = "target_nameorig")
    private String target_nameOrig;

    @Column(name = "path_number")
    private Integer path_number;

    @Column(name = "predecessor_oid")
    private String predecessor_oid;

    @Column(name = "predecessor_name")
    private String predecessor_name;

    @Column(name = "predecessor_displayname")
    private String predecessor_displayName;

    @Column(name = "predecessor_objecttype")
    private String predecessor_objectType;

    @Column(name = "predecessor_order")
    private String predecessor_order;

    @Column(name = "predecessor_archetype_name")
    private String predecessor_archetype_name;

    @Column(name = "predecessor_archetype_displayname")
    private String predecessor_archetype_displayName;

    @Column(name = "predecessor_archetype_plural")
    private String predecessor_archetype_plural;

    @Column(name = "predecessor_archetype_icon_class")
    private String predecessor_archetype_icon_class;

    @Column(name = "predecessor_archetype_icon_color")
    private String predecessor_archetype_icon_color;

    @Column(name = "target_displayname")
    private String target_displayName;

    @Column(name = "target_archetype_oid")
    private String target_archetype_oid;

    @Column(name = "target_archetype_nameorig")
    private String target_archetype_nameOrig;

    @Column(name = "assgn_validfrom")
    private LocalDateTime assgn_validFrom;

    @Column(name = "assgn_validto")
    private LocalDateTime assgn_validTo;

    @Column(name = "assgn_administrativestatus")
    private String assgn_administrativeStatus;

    @Column(name = "assgn_effectivestatus")
    private String assgn_effectiveStatus;

    @Column(name = "archetype_displayname")
    private String archetype_displayName;

    @Column(name = "archetype_plural")
    private String archetype_plural;

    @Column(name = "archetype_icon_class")
    private String archetype_icon_class;

    @Column(name = "archetype_icon_color")
    private String archetype_icon_color;
}
