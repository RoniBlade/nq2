package lct.tomorrowgen.model.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import lct.tomorrowgen.model.xlsx.HvsXlsxMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractKafkaMessage<T> {
    @Value("schema_version")
    private String schemaVersion = "1.0";

    @Value("record_type")
    private String recordType = "batch";

    @Value("sensor_id")
    private String sensorId = "hvs-ps1-001";

    @Value("asset_id")
    private String assetId = "ps1";

    @Value("ts")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private long timestamp = System.currentTimeMillis();

    @Value("is_late")
    private boolean isLate = false;

    @Value("rev")
    private int rev = 1;

    @Value("flow")
    private T flow;
}