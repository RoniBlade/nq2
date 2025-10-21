package org.example.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.ObjectTypeEnum;

import java.awt.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
//@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ObjectInfoLiteDto {

    private Long id;
    private UUID oid;
    private String objectType;
    private String objectName;
    private String objectFullName;
    private String objectDescription;
    private String userTitle;
    private String userEmailAddress;
    private String userTelephoneNumber;
    private String photo;
}
