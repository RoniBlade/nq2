package com.example.monitoring.repo;

import com.example.monitoring.domain.RuleParamsHvs;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HvsRepository extends JpaRepository<RuleParamsHvs, String> {

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO rule_params_hvs(sensor_id, hour_of_day, is_weekend,
        median_consumption, sigma_consumption, upper_consumption,
        median_cumulative, sigma_cumulative, updated_at)
    VALUES (:sensorId, :hour, :weekend, :mc, :sc, :uc, :mcu, :scu, now())
    ON CONFLICT (sensor_id, hour_of_day, is_weekend) DO UPDATE SET
        median_consumption = EXCLUDED.median_consumption,
        sigma_consumption  = EXCLUDED.sigma_consumption,
        upper_consumption  = EXCLUDED.upper_consumption,
        median_cumulative  = EXCLUDED.median_cumulative,
        sigma_cumulative   = EXCLUDED.sigma_cumulative,
        updated_at = now()
    """, nativeQuery = true)
    void upsert(String sensorId, int hour, boolean weekend,
                double mc, double sc, double uc, double mcu, double scu);

    // ---- новый метод ----
    @Query(value = """
        SELECT * FROM rule_params_hvs
        WHERE sensor_id = :sensorId
          AND hour_of_day = :hour
          AND is_weekend = :weekend
        LIMIT 1
        """, nativeQuery = true)
    RuleParamsHvs findCurrent(String sensorId, int hour, boolean weekend);
}
