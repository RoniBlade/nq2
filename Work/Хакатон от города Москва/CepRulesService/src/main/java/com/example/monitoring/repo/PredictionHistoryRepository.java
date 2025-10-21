package com.example.monitoring.repo;

import com.example.monitoring.entity.PredictionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PredictionHistoryRepository extends JpaRepository<PredictionHistory, Long> {
}
