package org.example.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "glt_case_info_v")
public class CaseInfoEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "caseoid")
    private UUID caseoid;

    @Column(name = "casedatecreate")
    private LocalDateTime caseDateCreate;

    @Column(name = "type")
    private String type;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_timestamp")
    private String eventTimeStamp;

    @Column(name = "event_initiator_oid")
    private String eventInitiatorOid;

    @Column(name = "event_initiator_name")
    private String eventInitiatorname;

    @Column(name = "event_initiator_displayname")
    private String eventInitiatorDisplayName;

    @Column(name = "event_initiator_relation")
    private String eventInitiatorRelation;

    @Column(name = "event_initiator_type")
    private String eventInitiatorType;

    @Column(name = "event_stagenumber")
    private String eventStageNumber;

    @Column(name = "event_delegationmethod")
    private String eventDelegationMethod;

    @Column(name = "event_externalworkitemid")
    private String eventExternalWorkItemId;

    @Column(name = "event_workitemid")
    private String eventWorkItemId;

    @Column(name = "event_escalation_level_number")
    private String eventEscalationLevelNumber;

    @Column(name = "event_escalation_level_name")
    private String eventEscalationLevelName;

    @Column(name = "event_delegated_to_oid")
    private String eventDelegatedToOid;

    @Column(name = "event_delegated_to_displayname")
    private String eventDelegatedToDisplayName;

    @Column(name = "event_delegated_to_relation")
    private String eventDelegatedToRelation;

    @Column(name = "event_delegated_to_type")
    private String eventDelegatedToType;

    @Column(name = "event_delegated_name")
    private String eventDelegatedName;

    @Column(name = "event_delegated_displayname")
    private String eventDelegatedDisplayName;

    @Column(name = "event_cause_type")
    private String eventCauseType;

    @Column(name = "event_output_outcome")
    private String eventOutputOutcome;

    @Column(name = "event_output_comment")
    private String eventOutputComment;

    @Column(name = "stage_id")
    private Integer stageId;

    @Column(name = "stage_name")
    private String stageName;

    @Column(name = "stage_number")
    private String stageNumber;

    @Column(name = "stage_groupexpansion")
    private String StageGroupExpansion;

    @Column(name = "stage_evaluationstrategy")
    private String stageEvaluationStrategy;

    @Column(name = "stage_outcomeifnoapprovers")
    private String stageOutcomeIfNoApprovers;

    @Column(name = "stage_approver_oid")
    private String stageApproverOid;

    @Column(name = "stage_approver_name")
    private String stageApproverName;

    @Column(name = "stage_approver_displayname")
    private String stageApproverDisplayName;

    @Column(name = "stage_approver_type")
    private String stageApproverType;

    @Column(name = "stage_approver_relation")
    private String stageApproverRelation;

    @Column(name = "assign_wi_oid")
    private Integer assignWiOid;

    @Column(name = "assign_user_oid")
    private UUID assignUserOid;

    @Column(name = "assign_namenorm")
    private String assignNameNorm;

    @Column(name = "assign_fullnameorig")
    private String assignFullNameOrig;

    @Column(name = "assign_state")
    private String assignState;

    @Column(name = "assign_outcome")
    private String assignOutcome;

    @Column(name = "assign_stagenumber")
    private String assignStageNumber;

}
