package com.example.monitoring.api;

import com.example.monitoring.domain.EvaluateRequest;
import com.example.monitoring.service.EvaluateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluate")
@RequiredArgsConstructor
public class EvaluateController {

    private final EvaluateService evaluateService;

    @PostMapping("/hvs/{sensorId}")
    public ResponseEntity<?> evaluateHvs(
            @PathVariable String sensorId,
            @RequestBody EvaluateRequest request) {
        return ResponseEntity.ok(evaluateService.evaluateHvs(sensorId, request));
    }

    @PostMapping("/gvs/{sensorId}")
    public ResponseEntity<?> evaluateGvs(
            @PathVariable String sensorId,
            @RequestBody EvaluateRequest request) {
        return ResponseEntity.ok(evaluateService.evaluateGvs(sensorId, request));
    }
}
