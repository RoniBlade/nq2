package com.example.monitoring.service;

import com.example.monitoring.domain.EvaluateRequest;
import com.example.monitoring.domain.RuleParamsGvs;
import com.example.monitoring.domain.RuleParamsHvs;
import com.example.monitoring.entity.PredictionHistory;
import com.example.monitoring.repo.GvsRepository;
import com.example.monitoring.repo.HvsRepository;
import com.example.monitoring.repo.PredictionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluateService {

    private final HvsRepository hvsRepo;
    private final GvsRepository gvsRepo;
    private final PredictionHistoryRepository historyRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    // --- HVS ---
    public Map<String, Object> evaluateHvs(String sensorId, EvaluateRequest req) {
        var now = ZonedDateTime.now();
        int hour = now.getHour();
        boolean weekend = now.getDayOfWeek().getValue() >= 6;

        log.info("[HVS] Evaluating sensorId={} at hour={} weekend={}", sensorId, hour, weekend);

        RuleParamsHvs rule = hvsRepo.findCurrent(sensorId, hour, weekend);
        log.debug("[HVS] Loaded rule params: {}", rule);

        return evaluate(sensorId, req, rule, "hvs");
    }

    // --- GVS ---
    public Map<String, Object> evaluateGvs(String sensorId, EvaluateRequest req) {
        var now = ZonedDateTime.now();
        int hour = now.getHour();
        boolean weekend = now.getDayOfWeek().getValue() >= 6;

        log.info("[GVS] Evaluating sensorId={} at hour={} weekend={}", sensorId, hour, weekend);

        RuleParamsGvs rule = gvsRepo.findCurrent(sensorId, hour, weekend);
        log.debug("[GVS] Loaded rule params: {}", rule);

        return evaluate(sensorId, req, rule, "gvs");
    }

    // --- Общее ---
    private Map<String, Object> evaluate(String sensorId, EvaluateRequest req, Object ruleParams, String type) {
        log.info("[{}] Start evaluation for sensorId={} input={}", type.toUpperCase(), sensorId, req);

        Map<String, Object> result = new HashMap<>();

        // --- RULE ---
        Map<String, Object> ruleEval = compareWithRules(req.getData(), ruleParams, req.getExclude());
        result.put("rule", ruleEval);

        log.debug("[{}] Rule evaluation result: {}", type.toUpperCase(), ruleEval);

        historyRepo.save(PredictionHistory.builder()
                .sensorId(sensorId)
                .source("rule")
                .inputData(req.getData())
                .prediction(ruleEval)
                .createdAt(ZonedDateTime.now())
                .build()
        );

        // --- ML ---
        String url = "http://localhost:8000/predict/" + sensorId;
        log.info("[{}] Sending request to ML: {}", type.toUpperCase(), url);
        Map<String, Object> mlRaw = restTemplate.postForObject(url, req.getData(), Map.class);

        log.debug("[{}] Raw ML response: {}", type.toUpperCase(), mlRaw);

        Map<String, Object> mlEval = compareWithMl(req.getData(), mlRaw, req.getExclude());
        result.put("ml", mlEval);

        log.debug("[{}] ML evaluation result: {}", type.toUpperCase(), mlEval);

        historyRepo.save(PredictionHistory.builder()
                .sensorId(sensorId)
                .source("ml")
                .inputData(req.getData())
                .prediction(mlEval)
                .createdAt(ZonedDateTime.now())
                .build()
        );

        log.info("[{}] Completed evaluation for sensorId={}", type.toUpperCase(), sensorId);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> compareWithMl(Map<String, Object> data, Map<String, Object> mlResponse, List<String> exclude) {
        Map<String, Object> result = new HashMap<>();

        if (mlResponse == null || !mlResponse.containsKey("prediction")) {
            log.warn("[ML] No prediction in response: {}", mlResponse);
            result.put("error", "no prediction from ML");
            return result;
        }

        Map<String, Object> prediction = (Map<String, Object>) mlResponse.get("prediction");

        for (String key : prediction.keySet()) {
            if (exclude.contains(key)) {
                log.debug("[ML] Excluding key {}", key);
                continue;
            }

            Double actual = toDouble(data.get(key));
            Double predicted = toDouble(prediction.get(key));

            if (actual == null || predicted == null) continue;

            double diff = actual - predicted;
            boolean ok = Math.abs(diff) <= 0.05 * Math.abs(predicted); // допустимое отклонение ±5%

            result.put(key, Map.of(
                    "actual", actual,
                    "predicted", predicted,
                    "diff", diff,
                    "ok", ok
            ));

            log.debug("[ML] Key={} actual={} predicted={} diff={} ok={}", key, actual, predicted, diff, ok);
        }

        result.put("meta", Map.of(
                "horizon", mlResponse.get("horizon"),
                "version", mlResponse.get("version"),
                "sensorId", mlResponse.get("sensorId")
        ));

        return result;
    }

    // --- Сравнение с RULE ---
    private Map<String, Object> compareWithRules(Map<String, Object> data, Object ruleParams, List<String> exclude) {
        Map<String, Object> result = new HashMap<>();

        if (ruleParams instanceof RuleParamsHvs hvs) {
            Double consumption = toDouble(data.get("consumption_for_period"));
            Double cumulative = toDouble(data.get("cumulative_consumption"));

            if (!exclude.contains("consumption_for_period") && consumption != null) {
                boolean ok = consumption <= hvs.getUpperConsumption();
                result.put("consumption_for_period", Map.of(
                        "actual", consumption,
                        "median", hvs.getMedianConsumption(),
                        "upper", hvs.getUpperConsumption(),
                        "diff", consumption - hvs.getMedianConsumption(),
                        "ok", ok
                ));
                log.debug("[RULE:HVS] consumption_for_period actual={} median={} upper={} ok={}",
                        consumption, hvs.getMedianConsumption(), hvs.getUpperConsumption(), ok);
            }

            if (!exclude.contains("cumulative_consumption") && cumulative != null) {
                boolean ok = Math.abs(cumulative - hvs.getMedianCumulative()) <= 3 * hvs.getSigmaCumulative();
                result.put("cumulative_consumption", Map.of(
                        "actual", cumulative,
                        "median", hvs.getMedianCumulative(),
                        "diff", cumulative - hvs.getMedianCumulative(),
                        "ok", ok
                ));
                log.debug("[RULE:HVS] cumulative_consumption actual={} median={} sigma={} ok={}",
                        cumulative, hvs.getMedianCumulative(), hvs.getSigmaCumulative(), ok);
            }
        }

        if (ruleParams instanceof RuleParamsGvs gvs) {
            Double t1 = toDouble(data.get("t1"));
            Double t2 = toDouble(data.get("t2"));
            Double supply = toDouble(data.get("supply"));
            Double returnVal = toDouble(data.get("return"));

            if (!exclude.contains("supply") && supply != null) {
                boolean ok = Math.abs(supply - gvs.getMedianSupply()) <= 3 * gvs.getSigmaSupply();
                result.put("supply", Map.of(
                        "actual", supply,
                        "median", gvs.getMedianSupply(),
                        "diff", supply - gvs.getMedianSupply(),
                        "ok", ok
                ));
                log.debug("[RULE:GVS] supply actual={} median={} sigma={} ok={}",
                        supply, gvs.getMedianSupply(), gvs.getSigmaSupply(), ok);
            }

            if (!exclude.contains("return") && returnVal != null) {
                boolean ok = Math.abs(returnVal - gvs.getMedianReturn()) <= 3 * gvs.getSigmaReturn();
                result.put("return", Map.of(
                        "actual", returnVal,
                        "median", gvs.getMedianReturn(),
                        "diff", returnVal - gvs.getMedianReturn(),
                        "ok", ok
                ));
                log.debug("[RULE:GVS] return actual={} median={} sigma={} ok={}",
                        returnVal, gvs.getMedianReturn(), gvs.getSigmaReturn(), ok);
            }

            if (!exclude.contains("dT") && t1 != null && t2 != null) {
                double dT = t1 - t2;
                boolean ok = Math.abs(dT - gvs.getMedianDt()) <= 3 * gvs.getSigmaDt();
                result.put("deltaT", Map.of(
                        "actual", dT,
                        "median", gvs.getMedianDt(),
                        "diff", dT - gvs.getMedianDt(),
                        "ok", ok
                ));
                log.debug("[RULE:GVS] deltaT actual={} median={} sigma={} ok={}",
                        dT, gvs.getMedianDt(), gvs.getSigmaDt(), ok);
            }
        }

        return result;
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
