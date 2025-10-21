package org.example.repository.hibernate.view;

import org.example.dto.view.UserProfileDto;
import org.example.entity.view.UserProfileEntity;
import org.example.entity.view.UserProfileExtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends
        JpaRepository<UserProfileEntity, Long>,
        JpaSpecificationExecutor<UserProfileEntity> {
    Optional<UserProfileEntity> findByOid(UUID oid);

}
