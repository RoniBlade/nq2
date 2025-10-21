package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.example.model.ObjectTypeEnum;

import java.util.UUID;

@Data
public class MenuParamDto {

    private Integer row_num;
    private String oid;
    private String oid_parentMenu;
    private String entryType;
    private Boolean visible;
    private String menuItem;
    private String sortOrder;
    private String icon;
    private String object;
    private String condition;
    private String objectType;
    private String archetype;
    private String tab;
    private String channel;
    @JsonProperty("vDisplayName")
    private String vDisplayName;
}
