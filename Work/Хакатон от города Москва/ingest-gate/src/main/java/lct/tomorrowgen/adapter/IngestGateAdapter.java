package lct.tomorrowgen.adapter;

import lct.tomorrowgen.kafka.KafkaHvsSender;
import lct.tomorrowgen.kafka.KafkaOdpuSender;
import lct.tomorrowgen.model.xlsx.HvsXlsxMessage;
import lct.tomorrowgen.model.xlsx.OdpuXlsxMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IngestGateAdapter {

    @Autowired
    private XlsxProcessor<HvsXlsxMessage> xlsxProcessor;

    @Autowired
    private XlsxProcessor<OdpuXlsxMessage> odpuProcessor;

    @Autowired
    private KafkaHvsSender kafkaHvsSender;

    @Autowired
    private KafkaOdpuSender kafkaOdpuSender;

    @Value("${xlsx.odpu}")
    private String odpuXlsxFilePath;

    @Value("${xlsx.hvs}")
    private String hvsXlsxFilePath;

    public void run() {
        try {
            xlsxProcessor.process(hvsXlsxFilePath, HvsXlsxMessage.class, kafkaHvsSender);
        } catch (Exception e) {
            log.error("Error while processing HVS xlsx file: {}", e.getMessage());
        }
        try {
            odpuProcessor.process(odpuXlsxFilePath, OdpuXlsxMessage.class, kafkaOdpuSender);
        } catch (Exception e) {
            log.error("Error while processing ODPU xlsx file: {}", e.getMessage());
        }
    }
}