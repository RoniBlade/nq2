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
@Table(name = "glt_user_personas_v")
public class UserPersonasEntity  {
    @Id
//    @Column(name = "id")
//    private Integer id;

    @Column(name = "owneroid")
    private UUID ownerOid;

    @Column(name = "targetoid")
    private UUID targetOid;

    @Column(name = "own_nameorig")
    private String own_nameOrig;

    @Column(name = "own_namenorm")
    private String own_nameNorm;

    @Column(name = "own_fullname")
    private String own_fullName;

    @Column(name = "per_nameorig")
    private String per_nameOrig;

    @Column(name = "per_namenorm")
    private String per_nameNorm;

    @Column(name = "per_fullname")
    private String per_fullName;

    @Column(name = "per_emailaddress")
    private String per_emailAddress;

    @Column(name = "per_personalnumber")
    private String per_personalNumber;

    @Column(name = "vdisplayname")
    private String vDisplayName; //

    @Column(name = "description")
    private String description;

    @Column(name = "target_archetype_oid")
    private UUID target_archetype_oid;

    @Column(name = "target_archetype_nameorig")
    private String target_archetype_nameOrig;

    @Column(name = "archetype_icon_class")
    private String archetype_icon_class;

    @Column(name = "archetype_icon_color")
    private String archetype_icon_color;
}
