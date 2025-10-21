package org.example.dto.view;

import io.swagger.models.auth.In;
import lombok.Data;

import java.util.UUID;

@Data
public class RoleProfileExtDto {

    private Integer id;
    private UUID oid;
    private String ext_attr_value;
    private String ext_attr_name;
    private String cardinality;
    private String type;
    private String contanerType;
}
