package com.example.monitoring.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EvaluateRequest {
    private Map<String, Object> data;
    private List<String> exclude;
}
