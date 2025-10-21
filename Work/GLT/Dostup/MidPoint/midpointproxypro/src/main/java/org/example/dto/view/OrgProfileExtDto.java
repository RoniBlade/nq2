package org.example.dto.view;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgProfileExtDto {

//    private Integer id;
    private UUID oid;
    private String extAttrValue;
    private String extAttrName;
    private String cardinality;
    private String type;
    private String contanerType;
}
