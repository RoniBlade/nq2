package com.example.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModeManager {

    private final KafkaRuleProcessor kafkaRuleProcessor;

    @Value("${app.kafka.topics.hour.hvs}")
    private String hvsHour;

    @Value("${app.kafka.topics.hour.gvs}")
    private String gvsHour;

    @Value("${app.kafka.topics.minute.hvs}")
    private String hvsMinute;

    @Value("${app.kafka.topics.minute.gvs}")
    private String gvsMinute;

    private String currentMode = "training";

    public String getMode() {
        return currentMode;
    }

    public String switchMode(String mode) {
        if (!mode.equals("training") && !mode.equals("prediction") && !mode.equals("idle")) {
            throw new IllegalArgumentException("Mode must be training, prediction or idle");
        }

        log.info("Switching mode from '{}' to '{}'", currentMode, mode);
        this.currentMode = mode;
        return currentMode;
    }

    /**
     * Запуск обработки в зависимости от режима.
     */
    public void processMessages() {
        switch (currentMode) {
            case "training" -> {
                log.info("Mode=training → processing hourly topics");
                kafkaRuleProcessor.processHvsHour(hvsHour);
                kafkaRuleProcessor.processGvsHour(gvsHour);
            }
            case "prediction" -> {
                log.info("Mode=prediction → TODO: implement minute processing");
                // тут можно потом сделать вызов processMinute(...), чтобы проверять минутные данные
            }
            case "idle" -> log.warn("Mode=idle → nothing to process");
            default -> throw new IllegalStateException("Unexpected mode: " + currentMode);
        }
    }
}
