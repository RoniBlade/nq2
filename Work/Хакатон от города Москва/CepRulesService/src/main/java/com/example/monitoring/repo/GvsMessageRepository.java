package com.example.monitoring.repo;

import com.example.monitoring.entity.GvsMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GvsMessageRepository extends JpaRepository<GvsMessageEntity, Long> {
    // здесь можно добавить методы поиска по sensorId, диапазону времени и т.д.
}
