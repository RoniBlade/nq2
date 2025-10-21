package lct.tomorrowgen.model.xlsx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@Slf4j
public class OdpuXlsxMessage implements XlsxInterface {
    @JsonProperty("date")
    private String date;
    @JsonProperty("time")
    private String time;
    @JsonProperty("supply")
    private Double supply;
    @JsonProperty("return")
    private Double returnValue;
    @JsonProperty("consumption_for_period")
    private Double consumptionForPeriod;
    @JsonProperty("t1")
    private Integer firstTemperature;
    @JsonProperty("t2")
    private Integer secondTemperature;

    @Override
    public void setField(int index, Object value) {
        try {
            log.debug("ODPU setField[{}]: raw={}, rawType={}", index,
                    value, value == null ? "null" : value.getClass().getSimpleName());

            switch (index) {
                case 0 -> { this.date = asDate(value); log.debug(" -> date={}", this.date); }
                case 1 -> { this.time = asHour(value); log.debug(" -> time={}", this.time); }
                case 2 -> { this.supply = asDouble(value); log.debug(" -> supply={}", this.supply); }
                case 3 -> { this.returnValue = asDouble(value); log.debug(" -> returnValue={}", this.returnValue); }
                case 4 -> { this.consumptionForPeriod = asDouble(value); log.debug(" -> consumptionForPeriod={}", this.consumptionForPeriod); }
                case 5 -> { this.firstTemperature = asInteger(value); log.debug(" -> firstTemperature={}", this.firstTemperature); }
                case 6 -> { this.secondTemperature = asInteger(value); log.debug(" -> secondTemperature={}", this.secondTemperature); }
            }
        } catch (Exception e) {
            log.error("ODPU setField error at index {} (value={}): {}", index, value, e.getMessage(), e);
            throw new RuntimeException("ODPU setField failed at index " + index, e);
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


    private static Integer asInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return null;
            s = s.replace(',', '.');
            Double d = Double.parseDouble(s);
            return d.intValue();
        }
        throw new IllegalArgumentException("Cannot convert " + v.getClass() + " to Integer");
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
