package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAssignmentsGrafDto {
    private String tab;
    private Boolean isRequest;
    private Long id;
    private UUID ownerOid;
    private UUID targetOid;
    private String targetType;
    private String targetNameOrig;
    private String targetDisplayName;
    private String targetArchetypeNameOrig;
    private String archetypeIconClass;
    private String archetypeIconColor;
}
