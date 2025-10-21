package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.ObjectTypeEnum;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ApprovalRequestDto {

    private Long id;
    private UUID caseOid;
    private Long workItemId;
    private LocalDateTime caseDateCreate;
    private UUID userOid;
    private String userName;
    private String userFullName;
    private String userTitle;
    private String userOrganization;
    private String userEmail;
    private String userPhone;
    private UUID requesterOid;
    private String requesterName;
    private String requesterFullName;
    private String requesterTitle;
    private String asRequesterOrganization;
    private String requesterEmail;
    private String requesterPhone;
    private UUID objectOid;
    private String objectName;
    private String objectDisplayName;
    private String objectDescription;
    private String objectType;
    private String state;
    private UUID oidApprover;
    private String nameApprover;
}
