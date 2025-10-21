package lct.tomorrowgen.model.kafka;

import lct.tomorrowgen.model.xlsx.OdpuXlsxMessage;
import lombok.Builder;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;

@ToString(callSuper = true)
@SuperBuilder
public class OdpuKafkaMessage extends AbstractKafkaMessage<OdpuXlsxMessage> {
}
