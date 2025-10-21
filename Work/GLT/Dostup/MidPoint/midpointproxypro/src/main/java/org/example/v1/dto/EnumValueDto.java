package org.example.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

/** DTO for d_enum_values */
@Data
public class EnumValueDto {
    private UUID oid;
    private String enumtype;
    private String enumvalue;
}
