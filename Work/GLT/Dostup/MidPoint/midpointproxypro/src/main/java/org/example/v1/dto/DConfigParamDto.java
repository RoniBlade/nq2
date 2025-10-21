package org.example.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class DConfigParamDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID oid;
    private UUID parentoid;
    private String configparam;
    private String value;
    private Boolean enabled;
    private String description;
    private String fullpath;
    private Integer sortorder;
    private String name;
}
