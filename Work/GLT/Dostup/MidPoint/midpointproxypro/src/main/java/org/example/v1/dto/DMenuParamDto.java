package org.example.v1.dto;

import lombok.Data;
import org.example.model.ObjectTypeEnum;

import java.util.UUID;

@Data
public class DMenuParamDto {
    private UUID oid;
    private UUID parentoid;
    private String entrytype;
    private Boolean visible;
    private String menuitem;
    private Integer sortorder;
    private String icon;
    private String displaytype;
    private String condition;
    private String objecttype;
    private String archetype;
    private String tab;
    private String channel;
}
