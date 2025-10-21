package org.example.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "glt_user_assignments_graf_v")
public class UserAssignmentsGrafEntity {

    @Column(name = "tab")
    private String tab;

    @Column(name = "isrequest")
    private Boolean isRequest;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "owneroid")
    private UUID ownerOid;

    @Column(name = "targetoid")
    private UUID targetOid;

    @Column(name = "targettype")
    private String targetType;

    @Column(name = "target_nameorig")
    private String targetNameOrig;

    @Column(name = "target_displayname")
    private String targetDisplayName;

    @Column(name = "target_archetype_nameorig")
    private String targetArchetypeNameOrig;

    @Column(name = "archetype_icon_class")
    private String archetypeIconClass;

    @Column(name = "archetype_icon_color")
    private String archetypeIconColor;
}
