package org.example.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFieldDto {

    private String name;
    private String type;
    private Object value;

    private String objectType;
    private String tableName;
    private String tableField;

    private Boolean visible;
    private Boolean read;

    private String tabName;
    private Integer extOrder;
    private String extType;
    private String extObject;
    private String extWhereClause;
    private String extNotes;

    private List<String> variables;

    @Setter
    private transient String displayNameValue;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDisplayNameValue() {
        return displayNameValue;
    }

    public UserFieldDto(String name,
                        String type,
                        Object value,
                        String objectType,
                        String tableField,
                        Boolean visible,
                        Boolean read,
                        String tabName,
                        Integer extOrder,
                        String extType,
                        String extObject,
                        String extWhereClause,
                        String extNotes,
                        List<String> variables) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.objectType = objectType;
        this.tableField = tableField;
        this.visible = visible;
        this.read = read;
        this.tabName = tabName;
        this.extOrder = extOrder;
        this.extType = extType;
        this.extObject = extObject;
        this.extWhereClause = extWhereClause;
        this.extNotes = extNotes;
        this.variables = variables;
    }



}
