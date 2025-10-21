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
public class CaseInfoDto {

    private Long id;
    private UUID caseoid;
    private LocalDateTime caseDateCreate;
    private String type;
    private String eventType;
    private String eventId;
    private String eventTimeStamp;
    private String eventInitiatorOid;
    private String eventInitiatorName;
    private String eventInitiatorDisplayName;
    private String eventInitiatorRelation;
    private String eventInitiatorType;
    private String eventStageNumber;
    private String eventDelegationMethod;
    private String eventExternalWorkItemId;
    private String eventWorkItemId;
    private String eventEscalationLevelNumber;
    private String eventEscalationLevelName;
    private String eventDelegatedToOid;
    private String eventDelegatedToDisplayName;
    private String eventDelegatedToRelation;
    private String eventDelegatedToType;
    private String eventDelegatedName;
    private String eventDelegatedDisplayName;
    private String eventCauseType;
    private String eventOutputOutcome;
    private String eventOutputComment;
    private Integer stageId;
    private String stageName;
    private String stageNumber;
    private String stageGroupExpansion;
    private String stageEvaluationStrategy;
    private String stageOutcomeIfNoApprovers;
    private String stageApproverOid;
    private String stageApproverName;
    private String stageApproverDisplayName;
    private String stageApproverType;
    private String stageApproverRelation;
    private Integer assignWiOid;
    private UUID assignUserOid;
    private String assignNameNorm;
    private String assignFullNameOrig;
    private String assignState;
    private String assignOutcome;
    private String assignStageNumber;
}
