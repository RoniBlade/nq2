package com.example.monitoring.domain;

import lombok.Data;

@Data
public class MinuteMsgHvs {
    private String schemaVersion;
    private String recordType;
    private String sensorId;
    private String assetId;
    private long timestamp;
    private int rev;
    private Flow flow;
    private boolean late;

    @Data
    public static class Flow {
        private String date;
        private String time;
        private double cumulative_consumption;
        private double consumption_for_period;
    }
}
