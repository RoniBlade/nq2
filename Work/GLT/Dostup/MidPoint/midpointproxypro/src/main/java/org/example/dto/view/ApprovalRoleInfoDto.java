package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApprovalRoleInfoDto {

    private Long id;
    private UUID owneroid;
    private String containerType;
    private String targetType;
    private String objectName;
    private String objectDisplayName;
    private String objectDescription;
}
