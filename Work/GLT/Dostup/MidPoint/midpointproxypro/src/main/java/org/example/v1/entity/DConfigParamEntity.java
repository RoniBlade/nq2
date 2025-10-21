package org.example.v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// DConfigParamEntity.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "d_config_param")
public class DConfigParamEntity {
    @Id
    @Column(name = "oid")
    @GeneratedValue
    private UUID oid;

    @Column(name = "parentoid")
    private UUID parentoid;

    @Column(name = "configparam")
    private String configparam;

    @Column(name = "value")
    private String value;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "description")
    private String description;

    @Column(name = "fullpath")
    private String fullpath;

    @Column(name = "sortorder")
    private Integer sortorder;

}
