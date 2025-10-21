package org.example.repository.hibernate.view;

import org.example.entity.view.OrgProfileExtEntity;
import org.example.entity.view.UserProfileExtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface UserProfileExtRepository extends JpaRepository<UserProfileExtEntity, Long>,
        JpaSpecificationExecutor<UserProfileExtEntity> {
    List<UserProfileExtEntity> findAllByOid(UUID oid);
}