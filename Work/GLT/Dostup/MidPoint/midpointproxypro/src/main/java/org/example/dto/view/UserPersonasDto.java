package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class UserPersonasDto {
//    private Integer id;
    private UUID ownerOid;
    private UUID targetOid;
    private String own_nameOrig;
    private String own_nameNorm;
    private String own_fullName;
    private String per_nameOrig;
    private String per_nameNorm;
    private String per_fullName;
    private String per_emailAddress;
    private String per_personalNumber;
    @JsonProperty("vDisplayName") private String vDisplayName;
    private String description;
    private UUID target_archetype_oid;
    private String target_archetype_nameOrig;
    private String archetype_icon_class;
    private String archetype_icon_color;
}
