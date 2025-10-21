package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.example.dto.StageDto;
import org.example.model.ObjectTypeEnum;

import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessCertActiveRequestDto {

    private Long id;
    private UUID campaignOid;
    private Long caseOid;
    private Long workItemId;
    private UUID objectId;
    private String objectType;
    private String objectName;
    private String objectDisplayName;
    private String objectDescription;
    private String objectTitle;
    private String objectOrg;

    private UUID targetId;
    private ObjectTypeEnum targetType;
    private String targetName;
    private String targetDisplayName;
    private String targetDescription;
    private String targetTitle;
    private String targetOrg;

    private String outcome;
    private Integer stageNumber;
    private Integer campaignIteration;
    private List<StageDto> stage;
    private UUID approver;
    private String certName;
}
