package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountAttributeDto {
    private Long id;
    private UUID oid;
    private String attrName;
    private String attrValue;
    private Boolean entitlements;
}
