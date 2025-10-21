package org.example.entity.view;

import jakarta.persistence.*;
import lombok.*;
import org.example.model.ObjectTypeEnum;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "d_menu_param_v")
public class MenuParamEntity {

    @Id
    @Column(name = "row_num")
    private Integer row_num;

    @Column(name = "oid")
    private String oid;

    @Column(name = "oid_parentmenu")
    private String oid_parentMenu;

    @Column(name = "entrytype")
    private String entryType;

    @Column(name = "visible")
    private Boolean visible;

    @Column(name = "menuitem")
    private String menuItem;

    @Column(name = "sortorder")
    private String sortOrder;

    @Column(name = "icon")
    private String icon;

    @Column(name = "object")
    private String object;

    @Column(name = "condition")
    private String condition;

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "archetype")
    private String archetype;

    @Column(name = "tab")
    private String tab;

    @Column(name = "channel")
    private String channel;

    @Column(name = "vdisplayname")
    private String vDisplayName;
}
