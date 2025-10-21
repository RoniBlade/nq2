package org.example.v1.repository;

import org.example.v1.entity.EnumValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnumValueRepository extends JpaRepository<EnumValueEntity, UUID>,
        JpaSpecificationExecutor<EnumValueEntity> {
    List<EnumValueEntity> findByEnumtype(String enumType);

    Optional<EnumValueEntity> findByOid(UUID oid);
    boolean existsByOid(UUID oid);
    @Modifying
    void deleteByOid(UUID oid);
}