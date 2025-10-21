package org.example.v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "d_object_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObjectTypeFieldEntity {

    @Id
    @Column(name = "oid")
    @GeneratedValue
    private UUID oid;

    @Column(name = "fieldname")
    private String fieldname;

    @Column(name = "fieldtype")
    private String fieldtype;
    @Column(name = "objecttype")
    private String objecttype;

    @Column(name = "archetype")
    private String archetype;

    @Column(name = "tablefield")
    private String tablefield;

    @Column(name = "send")
    private Boolean send;

    @Column(name = "visible")
    private Boolean visible;

    @Column(name = "read")
    private Boolean read;

    @Column(name = "tabname")
    private String tabname;

    @Column(name = "extorder")
    private Integer extorder;

    @Column(name = "exttype")
    private String exttype;

    @Column(name = "extobject")
    private String extobject;

    @Column(name = "extwhereclause")
    private String extwhereclause;

    @Column(name = "extnotes")
    private String extnotes;

    public ObjectTypeFieldEntity(String objectType, String fieldName, String fieldType) {
        this.objecttype = objectType;
        this.fieldname = fieldName;
        this.fieldtype = fieldType;
    }

}
