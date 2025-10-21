package org.example.repository.hibernate.view;

import org.example.entity.view.UserPersonasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPersonasRepository extends
        JpaRepository<UserPersonasEntity, Integer>,
        JpaSpecificationExecutor<UserPersonasEntity> {
}
