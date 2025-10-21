package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.example.model.ObjectTypeEnum;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyRequestDto {

    private Long id;
    private UUID caseOid;
    private LocalDateTime caseDateCreate;
    private String objectType;
    private String objectName;
    private String objectDisplayName;
    private String requesterName;
    private String requesterFullName;
    private String requesterTitle;
    private String asRequesterOrganization;
    private String requesterEmail;
    private String requesterPhone;
    private LocalDateTime closeTime;
    private String state;
    private UUID targetOid;
    private String targetNameNorm;
    private String objectId;

}
