package org.example.model.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FilterRequest {
    private List<FilterNode> filters;
    private List<String> fields;
    private List<String> excludeFields;

}
