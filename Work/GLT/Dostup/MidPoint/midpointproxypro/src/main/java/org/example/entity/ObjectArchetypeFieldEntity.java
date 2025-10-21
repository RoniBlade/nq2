package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "d_object_archetype_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObjectArchetypeFieldEntity {

    @Id
    private Long id;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "field_type")
    private String fieldType;

    @Column(name = "object_type")
    private String objectType;

    @Column(name = "ext_archetype")
    private String extArchetype;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "table_field")
    private String tableField;

    @Column(name = "send")
    private Boolean send;

    @Column(name = "visible")
    private Boolean visible;

    @Column(name = "read")
    private Boolean read;

    @Column(name = "tabname")
    private String tabName;

    @Column(name = "ext_order")
    private Integer extOrder;

    @Column(name = "ext_type")
    private String extType;

    @Column(name = "ext_object")
    private String extObject;

    @Column(name = "ext_whereclause")
    private String extWhereclause;

    @Column(name = "ext_notes")
    private String extNotes;
}
