package org.example.model.filter;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FilterCriterion {
    private String field;          // имя поля для фильтрации (например, "status")
    private FilterOperation op;    // оператор фильтрации (equal, greaterThan, lessThan, in, like)
    private Object value;          // значение для сравнения (или список значений для операции IN)

    public FilterCriterion() {}
    public FilterCriterion(String field, FilterOperation op, Object value) {
        this.field = field;
        this.op = op;
        this.value = value;
    }

}