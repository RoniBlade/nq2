package org.example.v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.ObjectTypeEnum;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "d_menu_param")
public class DMenuParamEntity {

    @Id
    @Column(name = "oid")
    @GeneratedValue
    private UUID oid;

    @Column(name = "parentoid")
    private UUID parentoid;

    @Column(name = "entrytype")
    private String entrytype;

    @Column(name = "visible")
    private Boolean visible;

    @Column(name = "menuitem")
    private String menuitem;

    @Column(name = "sortorder")
    private Integer sortorder;

    @Column(name = "icon")
    private String icon;

    @Column(name = "displaytype")
    private String displaytype;

    @Column(name = "condition")
    private String condition;

    @Column(name = "objecttype")
    private String objecttype;

    @Column(name = "archetype")
    private String archetype;

    @Column(name = "tab")
    private String tab;

    @Column(name = "channel")
    private String channel;

}
