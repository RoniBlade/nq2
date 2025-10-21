package org.example.repository.hibernate.view;

import org.example.entity.view.ConfigParamViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConfigParamViewRepository
        extends JpaRepository<ConfigParamViewEntity, Integer>,
        JpaSpecificationExecutor<ConfigParamViewEntity> {
}
