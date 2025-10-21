package com.example.monitoring.service;

import com.example.monitoring.domain.HourMsgGvs;
import com.example.monitoring.domain.HourMsgHvs;
import com.example.monitoring.entity.GvsMessageEntity;
import com.example.monitoring.entity.HvsMessageEntity;
import com.example.monitoring.repo.GvsMessageRepository;
import com.example.monitoring.repo.HvsMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaRuleProcessor {

    private final ObjectMapper objectMapper;
    private final RuleBuilderService ruleBuilderService;

    private final HvsMessageRepository hvsMessageRepo;
    private final GvsMessageRepository gvsMessageRepo;

    @Value("${app.kafka.bootstrap}")
    private String bootstrapServers;

    /**
     * Чтение HVS (часовые) → сохранение истории → вычисление правил.
     */
    public void processHvsHour(String topic) {
        List<HourMsgHvs> messages = readMessages(topic, HourMsgHvs.class);
        if (messages.isEmpty()) {
            log.warn("No HVS messages read from {}", topic);
            return;
        }

        // сохраняем историю
        messages.forEach(this::saveHvsMessage);

        // группируем по sensorId и строим правила
        Map<String, List<HourMsgHvs>> grouped = messages.stream()
                .collect(Collectors.groupingBy(HourMsgHvs::getSensorId));

        grouped.forEach((sensorId, sensorMsgs) ->
                ruleBuilderService.buildRulesForHvs(sensorId, sensorMsgs));
    }

    /**
     * Чтение GVS (часовые) → сохранение истории → вычисление правил.
     */
    public void processGvsHour(String topic) {
        List<HourMsgGvs> messages = readMessages(topic, HourMsgGvs.class);
        if (messages.isEmpty()) {
            log.warn("No GVS messages read from {}", topic);
            return;
        }

        // сохраняем историю
        messages.forEach(this::saveGvsMessage);

        // группируем по sensorId и строим правила
        Map<String, List<HourMsgGvs>> grouped = messages.stream()
                .collect(Collectors.groupingBy(HourMsgGvs::getSensorId));

        grouped.forEach((sensorId, sensorMsgs) ->
                ruleBuilderService.buildRulesForGvs(sensorId, sensorMsgs));
    }

    // ------------------- save helpers -------------------

    private void saveHvsMessage(HourMsgHvs msg) {
        try {
            HvsMessageEntity entity = HvsMessageEntity.builder()
                    .sensorId(msg.getSensorId())
                    .assetId(msg.getAssetId())
                    .timestamp(Instant.ofEpochMilli(msg.getTimestamp()))
                    .dateStr(msg.getFlow().getDate())
                    .timeStr(msg.getFlow().getTime())
                    .cumulativeConsumption(msg.getFlow().getCumulative_consumption())
                    .consumptionForPeriod(msg.getFlow().getConsumption_for_period())
                    .build();
            hvsMessageRepo.save(entity);
        } catch (Exception e) {
            log.error("Failed to save HVS message: {}", msg, e);
        }
    }

    private void saveGvsMessage(HourMsgGvs msg) {
        try {
            GvsMessageEntity entity = GvsMessageEntity.builder()
                    .sensorId(msg.getSensorId())
                    .assetId(msg.getAssetId())
                    .timestamp(Instant.ofEpochMilli(msg.getTimestamp()))
                    .dateStr(msg.getFlow().getDate())
                    .timeStr(msg.getFlow().getTime())
                    .supply(msg.getFlow().getSupply())
                    .returnVal(msg.getFlow().getReturnVal())
                    .consumptionForPeriod(msg.getFlow().getConsumption_for_period())
                    .t1(msg.getFlow().getT1())
                    .t2(msg.getFlow().getT2())
                    .build();
            gvsMessageRepo.save(entity);
        } catch (Exception e) {
            log.error("Failed to save GVS message: {}", msg, e);
        }
    }

    // ------------------- kafka helpers -------------------

    private <T> List<T> readMessages(String topic, Class<T> clazz) {
        Properties props = baseProps("reader-" + topic, "earliest", false);
        List<T> result = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));

            int emptyPolls = 0;
            int maxEmptyPolls = 5;

            while (emptyPolls < maxEmptyPolls) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                emptyPolls = 0;

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        T msg = objectMapper.readValue(record.value(), clazz);
                        result.add(msg);
                    } catch (Exception e) {
                        log.error("Failed to parse {}: {}", clazz.getSimpleName(), record.value(), e);
                    }
                }
            }
        }

        log.info("Read {} messages from topic {}", result.size(), topic);
        return result;
    }

    private Properties baseProps(String groupId, String offset, boolean autoCommit) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(autoCommit));
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        return props;
    }
}
