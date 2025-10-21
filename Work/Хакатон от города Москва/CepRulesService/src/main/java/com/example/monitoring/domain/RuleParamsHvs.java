package com.example.monitoring.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "rule_params_hvs")
public class RuleParamsHvs {
    @Id
    private String sensorId;

    private short  hourOfDay;
    private boolean isWeekend;

    private double medianConsumption;
    private double sigmaConsumption;
    private double upperConsumption;
    private double medianCumulative;
    private double sigmaCumulative;

    private Instant updatedAt;
}

