package org.example.dto.view;

import lombok.Data;

import java.util.UUID;

@Data
public class UserProfileExtDto {
    private Long id;
    private UUID oid;
    private String extAttrName;
    private String extAttrValue;
    private String cardinality;
    private String type;
    private String contanerType;
}