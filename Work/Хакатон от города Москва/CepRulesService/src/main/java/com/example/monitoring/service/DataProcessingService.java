package com.example.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataProcessingService {

    private final KafkaRuleProcessor kafkaReaderService;

    @Value("${app.kafka.topics.hour.hvs}")
    private String hvsHour;

    @Value("${app.kafka.topics.hour.gvs}")
    private String gvsHour;

    @Value("${app.kafka.topics.minute.hvs}")
    private String hvsMinute;

    @Value("${app.kafka.topics.minute.gvs}")
    private String gvsMinute;

    private String currentMode = "idle";

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
     * Запускаем обработку сообщений в зависимости от режима
     */
    public void processMessages() {
        if (currentMode.equals("training")) {
            kafkaReaderService.processHvsHour(hvsHour);
            kafkaReaderService.processGvsHour(gvsHour);
        } else if (currentMode.equals("prediction")) {
            log.info("Prediction mode not implemented yet (minute topics: {}, {})", hvsMinute, gvsMinute);
            // потом сделаем вызовы processMinute(...)
        } else {
            log.warn("Mode is idle → nothing to process");
        }
    }
}
