package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "d_config_param_v")
public class ConfigParamViewEntity {

    @Id
    @Column(name = "row_num")
    private Integer row_num;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "oid_parentconfigparam")
    private UUID parentConfigParamOid;

    @Column(name = "configparam")
    private String configParam;

    @Column(name = "value")
    private String value;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "description")
    private String description;

    @Column(name = "fullpath")
    private String fullPath;

    @Column(name = "sortorder")
    private Integer sortOrder;
}
