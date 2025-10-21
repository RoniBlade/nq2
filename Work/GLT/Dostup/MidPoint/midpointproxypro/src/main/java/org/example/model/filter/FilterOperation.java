package org.example.model.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FilterOperation {
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN,
    LESS_THAN,
    GREATER_OR_EQUAL,
    LESS_OR_EQUAL,
    IN,
    NOT_IN,
    SUBSTRING,
    NOT_SUBSTRING,
    STARTS_WITH,
    ENDS_WITH,
    NOT_NULL,
    IS_NULL;

    @JsonCreator
    public static FilterOperation fromString(String key) {
        return key == null ? null : FilterOperation.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }

}

