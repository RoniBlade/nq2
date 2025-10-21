package org.example.dto;

import lombok.Data;

@Data
public class AttributeDelta {
    private String name;
    private Object value;
    private String modification; // add, delete, replace
}
