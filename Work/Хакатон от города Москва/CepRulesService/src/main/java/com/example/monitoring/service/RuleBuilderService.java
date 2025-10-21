package com.example.monitoring.service;

import com.example.monitoring.domain.HourMsgHvs;
import com.example.monitoring.domain.HourMsgGvs;
import com.example.monitoring.repo.HvsRepository;
import com.example.monitoring.repo.GvsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleBuilderService {

    private final HvsRepository hvsRepo;
    private final GvsRepository gvsRepo;

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    /**
     * Обработка ХВС — формируем правила по часам (и будни/выходные) за 7 дней
     */
    public void buildRulesForHvs(String sensorId, List<HourMsgHvs> messages) {
        if (messages.isEmpty()) {
            log.warn("No HVS data for {}", sensorId);
            return;
        }

        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        List<HourMsgHvs> history = messages.stream()
                .filter(m -> Instant.ofEpochMilli(m.getTimestamp()).isAfter(cutoff))
                .toList();

        if (history.isEmpty()) {
            log.warn("No HVS history for {} in last 7 days", sensorId);
            return;
        }

        // группировка по (час из flow.time, будни/выходные)
        Map<HourKey, List<HourMsgHvs>> grouped = history.stream().collect(
                Collectors.groupingBy(m -> {
                    int hour = parseHour(m.getFlow().getTime());
                    boolean weekend = Instant.ofEpochMilli(m.getTimestamp())
                            .atZone(ZONE)
                            .getDayOfWeek()
                            .getValue() >= 6;
                    return new HourKey(hour, weekend);
                })
        );

        grouped.forEach((key, group) -> {
            List<Double> consumptions = group.stream()
                    .map(m -> m.getFlow().getConsumption_for_period())
                    .toList();
            List<Double> cumulatives = group.stream()
                    .map(m -> m.getFlow().getCumulative_consumption())
                    .toList();

            double medianConsumption = median(consumptions);
            double sigmaConsumption  = sigma(consumptions);
            double upperConsumption  = medianConsumption + 3.0 * sigmaConsumption;
            double medianCumulative  = median(cumulatives);
            double sigmaCumulative   = sigma(cumulatives);

            hvsRepo.upsert(
                    sensorId,
                    key.hour(),
                    key.weekend(),
                    medianConsumption,
                    sigmaConsumption,
                    upperConsumption,
                    medianCumulative,
                    sigmaCumulative
            );

            log.info("HVS rules [{} h={} weekend={}]: mc={} sc={} mcu={} scu={}",
                    sensorId, key.hour(), key.weekend(),
                    medianConsumption, sigmaConsumption, medianCumulative, sigmaCumulative);
        });
    }

    /**
     * Обработка ГВС — формируем правила по часам (и будни/выходные) за 7 дней
     */
    public void buildRulesForGvs(String sensorId, List<HourMsgGvs> messages) {
        if (messages.isEmpty()) {
            log.warn("No GVS data for {}", sensorId);
            return;
        }

        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        List<HourMsgGvs> history = messages.stream()
                .filter(m -> Instant.ofEpochMilli(m.getTimestamp()).isAfter(cutoff))
                .toList();

        if (history.isEmpty()) {
            log.warn("No GVS history for {} in last 7 days", sensorId);
            return;
        }

        // группировка по (час из flow.time, будни/выходные)
        Map<HourKey, List<HourMsgGvs>> grouped = history.stream().collect(
                Collectors.groupingBy(m -> {
                    int hour = parseHour(m.getFlow().getTime());
                    boolean weekend = Instant.ofEpochMilli(m.getTimestamp())
                            .atZone(ZONE)
                            .getDayOfWeek()
                            .getValue() >= 6;
                    return new HourKey(hour, weekend);
                })
        );

        grouped.forEach((key, group) -> {
            List<Double> supplies = group.stream()
                    .map(m -> m.getFlow().getSupply())
                    .toList();
            List<Double> returns = group.stream()
                    .map(m -> m.getFlow().getReturnVal())
                    .toList();
            List<Double> deltas = group.stream()
                    .map(m -> m.getFlow().getT1() - m.getFlow().getT2())
                    .toList();

            double medianSupply = median(supplies);
            double sigmaSupply  = sigma(supplies);
            double medianReturn = median(returns);
            double sigmaReturn  = sigma(returns);
            double medianDt     = median(deltas);
            double sigmaDt      = sigma(deltas);

            gvsRepo.upsert(
                    sensorId,
                    key.hour(),
                    key.weekend(),
                    medianSupply,
                    sigmaSupply,
                    medianReturn,
                    sigmaReturn,
                    medianDt,
                    sigmaDt
            );

            log.info("GVS rules [{} h={} weekend={}]: ms={} ss={} mr={} sr={} md={} sd={}",
                    sensorId, key.hour(), key.weekend(),
                    medianSupply, sigmaSupply, medianReturn, sigmaReturn, medianDt, sigmaDt);
        });
    }

    // ---------------- helpers ----------------

    private int parseHour(String timeStr) {
        if (timeStr == null) return 0;
        try {
            return Integer.parseInt(timeStr.split(":")[0]); // "1:00" -> 1
        } catch (Exception e) {
            return 0;
        }
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) return 0;
        var sorted = values.stream().sorted().toList();
        int n = sorted.size();
        if (n % 2 == 0) {
            return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        } else {
            return sorted.get(n / 2);
        }
    }

    /**
     * Робастная σ через MAD (sigma_r = 1.4826 * MAD)
     */
    private double sigma(List<Double> values) {
        if (values.isEmpty()) return 0;
        double med = median(values);
        List<Double> deviations = values.stream().map(v -> Math.abs(v - med)).toList();
        double mad = median(deviations);
        return 1.4826 * mad;
    }

    private record HourKey(int hour, boolean weekend) {}
}
