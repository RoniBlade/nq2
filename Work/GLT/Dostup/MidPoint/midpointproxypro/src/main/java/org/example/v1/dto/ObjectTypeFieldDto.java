package org.example.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObjectTypeFieldDto {
    private UUID oid;

    /** значение из результата (по table_field), как строка */
    private Object value;

    /** человекочитаемое значение (по умолчанию = value; для lookup можно подставлять имя) */
    private Object displayvalue;

    /** список возможных значений для lookup/enum (опционально) */
    private List<String> values;

    private String fieldname;
    private String fieldtype;
    private String objecttype;
    private String archetype;
    private String tablefield;
    private Boolean send;
    private Boolean visible;
    private Boolean read;
    private String tabname;
    private Integer extorder;
    private String exttype;
    private String extobject;
    private String extwhereclause;
    private String extnotes;


}
