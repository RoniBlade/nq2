package com.example.monitoring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "gvs_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GvsMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "ts", nullable = false)
    private Instant timestamp;

    @Column(name = "date_str")
    private String dateStr;

    @Column(name = "time_str")
    private String timeStr;

    @Column(name = "supply")
    private Double supply;

    @Column(name = "return_val")
    private Double returnVal;

    @Column(name = "consumption_for_period")
    private Double consumptionForPeriod;

    @Column(name = "t1")
    private Double t1;

    @Column(name = "t2")
    private Double t2;
}
