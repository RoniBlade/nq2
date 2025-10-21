package com.example.monitoring.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HourMsgGvs {
    private String sensorId;
    private String assetId;
    private long timestamp;
    private Flow flow;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flow {
        private String date;
        private String time;
        private double supply;
        @JsonProperty("return")
        private double returnVal;
        private double consumption_for_period;
        private double t1;
        private double t2;
    }
}
