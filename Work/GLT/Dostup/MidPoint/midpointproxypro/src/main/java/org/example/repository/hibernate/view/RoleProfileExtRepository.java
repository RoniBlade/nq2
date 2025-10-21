package org.example.repository.hibernate.view;

import org.example.dto.view.RoleProfileExtDto;
import org.example.entity.view.RoleProfileExtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleProfileExtRepository extends
        JpaRepository<RoleProfileExtEntity, Integer>,
        JpaSpecificationExecutor<RoleProfileExtEntity> {
}
