package lct.tomorrowgen.model.xlsx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@Slf4j
public class HvsXlsxMessage implements XlsxInterface {
    @JsonProperty("date")
    private String date;
    @JsonProperty("time")
    private String time;
    @JsonProperty("cumulative_consumption")
    private double cumulativeConsumption;
    @JsonProperty("consumption_for_period")
    private double consumptionForPeriod;
    @Override
    public void setField(int index, Object value) {
        try {
            log.debug("HVS setField[{}]: raw={}, rawType={}", index,
                    value, value == null ? "null" : value.getClass().getSimpleName());

            switch (index) {
                case 0 -> { this.date = asDate(value); log.debug(" -> date={}", this.date); }
                case 1 -> { this.time = asHour(value); log.debug(" -> time={}", this.time); }
                case 2 -> { this.cumulativeConsumption = asDouble(value); log.debug(" -> cumulativeConsumption={}", this.cumulativeConsumption); }
                case 3 -> { this.consumptionForPeriod = asDouble(value); log.debug(" -> consumptionForPeriod={}", this.consumptionForPeriod); }
            }
        } catch (Exception e) {
            log.error("HVS setField error at index {} (value={}): {}", index, value, e.getMessage(), e);
            throw new RuntimeException("HVS setField failed at index " + index, e);
        }
    }

    private static String asString(Object v) {
        if (v == null) return null;
        return v.toString().trim();
    }

    private static Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            s = s.trim().replace(",", ".");
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        }
        throw new IllegalArgumentException("Cannot convert " + v.getClass() + " to Double");
    }

    private static String asDate(Object v) {
        if (v == null) return null;

        if (v instanceof Date d) {
            return new SimpleDateFormat("dd.MM.yyyy").format(d);
        }

        if (v instanceof Number n) {
            Date d = DateUtil.getJavaDate(n.doubleValue());
            return new SimpleDateFormat("dd.MM.yyyy").format(d);
        }

        if (v instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return null;
            return s;
        }

        throw new IllegalArgumentException("Cannot convert " + v.getClass() + " to Date string");
    }

    private static String asHour(Object v) {
        if (v == null) return null;

        if (v instanceof Number n) {
            return n.intValue() + ":00";
        }

        if (v instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return null;

            String[] parts = s.split("-");
            if (parts.length > 0) {
                return parts[0].trim() + ":00";
            }
            return s + ":00";
        }

        if (v instanceof java.util.Date d) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(d);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            return hour + ":00";
        }

        throw new IllegalArgumentException("Cannot convert " + v.getClass() + " to hour string");
    }
}
