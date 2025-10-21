package lct.tomorrowgen.model.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import lct.tomorrowgen.model.xlsx.HvsXlsxMessage;
import lombok.Builder;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;

@ToString(callSuper = true)
@SuperBuilder
public class HvsKafkaMessage extends AbstractKafkaMessage<HvsXlsxMessage> {
}
