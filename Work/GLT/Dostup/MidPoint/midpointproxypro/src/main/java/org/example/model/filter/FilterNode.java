package org.example.model.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(example = """
                    {
                        "and": [
                            {
                                "field": "string",
                                "operation": "enum",
                                "value": ""
                            }
                        ]
                    }
            """)
public class FilterNode {
    private String logic;
    private List<FilterNode> filters;
    private FilterCriterion criterion;


    public boolean isLeaf() {
        return criterion != null;
    }


    @JsonCreator
    public FilterNode(@JsonProperty("logic") String logic,
                      @JsonProperty("filters") List<FilterNode> filters,
                      @JsonProperty("criterion") FilterCriterion criterion,
                      @JsonProperty("field") String field,
                      @JsonProperty("op") FilterOperation op,
                      @JsonProperty("operation") FilterOperation opFull,
                      @JsonProperty("value") Object value,
                      @JsonProperty("and") List<FilterNode> and,
                      @JsonProperty("or") List<FilterNode> or) {

        if(op == null) op = opFull;

        if (and != null) {
            this.logic = "and";
            this.filters = and;
        } else if (or != null) {
            this.logic = "or";
            this.filters = or;
        } else if (logic != null && filters != null) {
            this.logic = logic;
            this.filters = filters;
        } else if (field != null && op != null) {
            this.criterion = new FilterCriterion();
            this.criterion.setField(field);
            this.criterion.setOp(op);
            this.criterion.setValue(value);
        } else if (criterion != null) {
            this.criterion = criterion;
        }
    }
    public FilterNode(String field, FilterOperation op, Object value) {
        this.criterion = new FilterCriterion();
        this.criterion.setField(field);
        this.criterion.setOp(op);
        this.criterion.setValue(value);
    }
    public FilterNode() {}
}
