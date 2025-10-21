package com.glt.connector.service.sheduler;

import com.glt.connector.service.kafka.AccountConsumer;
import com.glt.connector.service.kafka.ScanTaskCreator;
import com.glt.connector.service.kafka.ScanTaskWorker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Scheduler {

    private final ScanTaskCreator scanTaskCreator;
    private final ScanTaskWorker scanTaskWorker;
    private final AccountConsumer accountConsumer;

    @Value("${connector.node.main:true}")
    private boolean isMainNode;

    @Value("${connector.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @PostConstruct
    public void init() {
        if (!kafkaEnabled) {
            log.warn("[SCHEDULER] Kafka отключена — процессы сканирования не будут запущены");
            return;
        }

        // стартуем worker’ы/consumer’ы ровно один раз
        log.info("[SCHEDULER] Запуск worker и consumer (однократно)...");
        scanTaskWorker.start();
        accountConsumer.start();

        if (isMainNode) {
            log.info("[SCHEDULER] Main-нода: инициализируем продьюсер (однократно)...");
            scanTaskCreator.start();   // только создаёт пул/ресурсы, без бесконечных циклов
            // при желании можно сразу инициировать первый проход:
            // scanTaskCreator.scanOnce();
        } else {
            log.info("[SCHEDULER] Secondary-нода: создание задач сканирования отключено");
        }
    }

    @Scheduled(fixedDelayString = "${connector.scheduler.fixedDelay}")
    public void scheduledScan() {
        if (!kafkaEnabled || !isMainNode) return;

        log.info("[SCHEDULER] Плановый запуск одноразового прохода сканирования...");
        // ВАЖНО: не start(), а одноразовый проход
        scanTaskCreator.scanOnce();
    }
}
