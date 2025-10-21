package lct.tomorrowgen.kafka;

import lct.tomorrowgen.config.SendersInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaHvsSender implements SendersInterface {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.hvs.topic-name}")
    private String TOPIC;

    public void send(String message) {
        kafkaTemplate.send(TOPIC, message);
    }

    public String getTopic() {
        return TOPIC;
    }
}