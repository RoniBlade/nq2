package lct.tomorrowgen.mapper;

import lct.tomorrowgen.model.kafka.HvsKafkaMessage;
import lct.tomorrowgen.model.kafka.OdpuKafkaMessage;
import lct.tomorrowgen.model.xlsx.HvsXlsxMessage;
import lct.tomorrowgen.model.xlsx.OdpuXlsxMessage;
import lct.tomorrowgen.model.xlsx.XlsxInterface;
import org.springframework.stereotype.Component;

@Component
public class XlsxKafkaMapper {

    public Object toKafkaMessage(XlsxInterface xlsxMessage) {
        if (xlsxMessage instanceof HvsXlsxMessage hvs) {
            return HvsKafkaMessage.builder()
                    .schemaVersion("1.0")
                    .recordType("batch")
                    .sensorId("hvs-ps1-001")
                    .assetId("ps1")
                    .timestamp(System.currentTimeMillis())
                    .isLate(false)
                    .rev(1)
                    .flow(hvs)
                    .build();
        } else if (xlsxMessage instanceof OdpuXlsxMessage odpu) {
            return OdpuKafkaMessage.builder()
                    .schemaVersion("1.0")
                    .recordType("batch")
                    .sensorId("odpu-ps1-001")
                    .assetId("ps1")
                    .timestamp(System.currentTimeMillis())
                    .isLate(false)
                    .rev(1)
                    .flow(odpu)
                    .build();
        } else {
            throw new RuntimeException("Unknown message type: " + xlsxMessage.getClass().getName());
        }
    }
}

