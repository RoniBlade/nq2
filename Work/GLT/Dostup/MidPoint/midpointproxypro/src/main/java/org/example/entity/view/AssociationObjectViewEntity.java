package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "glt_get_association_object_v")
public class AssociationObjectViewEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "nameorig")
    private String nameOrig;

    @Column(name = "v_displayname")
    private String displayName;

}
