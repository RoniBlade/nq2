package org.example.repository.hibernate.view;

import org.example.entity.view.RoleProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleProfileRepository extends
        JpaRepository<RoleProfileEntity, Long>,
        JpaSpecificationExecutor<RoleProfileEntity> {

}
