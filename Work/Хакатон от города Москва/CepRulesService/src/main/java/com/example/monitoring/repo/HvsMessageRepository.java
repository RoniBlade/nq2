package com.example.monitoring.repo;

import com.example.monitoring.entity.HvsMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HvsMessageRepository extends JpaRepository<HvsMessageEntity, Long> {
    // здесь можно добавить методы поиска по sensorId, диапазону времени и т.д.
}
