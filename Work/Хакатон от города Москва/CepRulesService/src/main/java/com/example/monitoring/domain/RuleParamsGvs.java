package com.example.monitoring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "rule_params_gvs")
public class RuleParamsGvs {
    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Column(name = "hour_of_day")
    private short  hourOfDay;

    @Column(name = "is_weekend")
    private boolean isWeekend;

    @Column(name = "median_supply")
    private double medianSupply;

    @Column(name = "sigma_supply")
    private double sigmaSupply;

    @Column(name = "median_return")
    private double medianReturn;

    @Column(name = "sigma_return")
    private double sigmaReturn;

    @Column(name = "median_dt")
    private double medianDt;

    @Column(name = "sigma_dt")
    private double sigmaDt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
