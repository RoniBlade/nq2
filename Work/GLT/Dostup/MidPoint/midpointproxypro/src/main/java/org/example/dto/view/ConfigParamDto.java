package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigParamDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID oid;
    private UUID parentConfigParamOid;
    private String configParam;
    private String value;
    private Boolean enabled;
    private String description;
    private String fullPath;
    private Integer sortOrder;
    private String name;
    @JsonProperty("vDisplayName")
    private String vDisplayName;
}
