package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationDto {

    private String dep_oid;
    private String dep_fullName;
    private String dep_personalNumber;
    private String dep_emailAddress;
    private String del_oid;
    private String del_fullName;
    private String del_personalNumber;
    private String del_emailAddress;
    private String description;
    private String effectiveStatus;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean allowTransitive;
    private Boolean certificationWorkItems;
    private Boolean caseManagementWorkItems;
    private String delegated_item_oid;
    private String delegated_item_type;
    private String delegated_item_relation;
    private String delegated_item_nameOrig;
    private String delegated_item_objectType;
    private String delegated_item_displayName;
    private String delegated_item_description;
    private String delegated_item_archetype_name;
    private String delegated_item_archetype_displayName;
    private String delegated_item_archetype_plural;
    private String delegated_item_archetype_icon_class;
    private String delegated_item_archetype_icon_color;

}
