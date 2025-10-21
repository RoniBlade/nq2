package org.larb.tedo.Model;

import lombok.Data;

@Data
public class TableStructureEntity {
    String columnName = "";
    String columnType = "";
    Integer charMaxLength = 0;
    Integer numericPrecision = 0;
    Integer numericRadix = 0;
    Integer numericScale = 0;
    Boolean isNullable = true;

}
