package com.example.monitoring.repo;

import com.example.monitoring.domain.RuleParamsGvs;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GvsRepository extends JpaRepository<RuleParamsGvs, String> {

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO rule_params_gvs(sensor_id, hour_of_day, is_weekend, 
                                    median_supply, sigma_supply, 
                                    median_return, sigma_return, 
                                    median_dt, sigma_dt, updated_at)
        VALUES (:sensorId, :hourOfDay, :isWeekend, :ms, :ss, :mr, :sr, :md, :sd, now())
        ON CONFLICT (sensor_id, hour_of_day, is_weekend) DO UPDATE SET
            median_supply = EXCLUDED.median_supply,
            sigma_supply  = EXCLUDED.sigma_supply,
            median_return = EXCLUDED.median_return,
            sigma_return  = EXCLUDED.sigma_return,
            median_dt     = EXCLUDED.median_dt,
            sigma_dt      = EXCLUDED.sigma_dt,
            updated_at = now()
        """, nativeQuery = true)
    void upsert(String sensorId, int hourOfDay, boolean isWeekend,
                double ms, double ss, double mr, double sr, double md, double sd);

    // ---- новый метод ----
    @Query(value = """
        SELECT * FROM rule_params_gvs
        WHERE sensor_id = :sensorId
          AND hour_of_day = :hour
          AND is_weekend = :weekend
        LIMIT 1
        """, nativeQuery = true)
    RuleParamsGvs findCurrent(String sensorId, int hour, boolean weekend);
}
