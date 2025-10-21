package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAssignmentsDto {
    private Integer id;
    private String tab;
    private Boolean isRequest;
    private String requestor_oid;
    private String requestor_name;
    private String requestor_displayName;
    private String requestor_personalNumber;
    private UUID ownerOid;
    private String ownerType;
    private String owner_nameOrig;
    private String owner_displayName;
    private UUID targetOid;
    private String targetType;
    private String target_nameOrig;
    private Integer path_number;
    private String predecessor_oid;
    private String predecessor_name;
    private String predecessor_displayName;
    private String predecessor_objectType;
    private String predecessor_order;
    private String predecessor_archetype_name;
    private String predecessor_archetype_displayName;
    private String predecessor_archetype_plural;
    private String predecessor_archetype_icon_class;
    private String predecessor_archetype_icon_color;
    private String target_displayName;
    private String target_archetype_oid;
    private String target_archetype_nameOrig;
    private LocalDateTime assgn_validFrom;
    private LocalDateTime assgn_validTo;
    private String assgn_administrativeStatus;
    private String assgn_effectiveStatus;
    private String archetype_displayName;
    private String archetype_plural;
    private String archetype_icon_class;
    private String archetype_icon_color;

}
