package org.example.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObjectArchetypeFieldDto {

    private Long id;

    private Object value;

    private String fieldName;

    private String fieldType;

    private String objectType;

    private String extArchetype;

    private String tableName;

    private String tableField;

    private Boolean send;

    private Boolean visible;

    private Boolean read;

    private String tabName;

    private String extOrder;

    private String extType;

    private String extObject;

    private String extWhereclause;

    private String extNotes;

    private List<String> variables;


    @Override
    public String toString() {
        return "ObjectArchetypeFieldDto{" +
                "id=" + id +
                ", value='" + value + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                ", objectType='" + objectType + '\'' +
                ", extArchetype='" + extArchetype + '\'' +
                ", tableName='" + tableName + '\'' +
                ", tableField='" + tableField + '\'' +
                ", send=" + send +
                ", visible=" + visible +
                ", read=" + read +
                ", tabName='" + tabName + '\'' +
                ", extOrder='" + extOrder + '\'' +
                ", extType='" + extType + '\'' +
                ", extObject='" + extObject + '\'' +
                ", extWhereclause='" + extWhereclause + '\'' +
                ", extNotes='" + extNotes + '\'' +
                ", variables=" + variables +
                '}';
    }

}
