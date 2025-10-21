package org.example.dto.view;

import lombok.Data;

import java.util.UUID;

@Data
public class AssignRoleInfoDto {

//    private Integer id;
    private UUID ownerOid;
    private String type;
    private String containerType;
    private UUID targetOid;
    private String object_description;
    private String kind;
    private String intent;
}
