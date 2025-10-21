package org.example.repository.hibernate;

import org.example.entity.view.ConfigParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ConfigParamRepository
        extends JpaRepository<ConfigParamEntity, UUID>,
        JpaSpecificationExecutor<ConfigParamEntity> {
}
