package org.example.v1.repository;

import org.example.v1.entity.DMenuParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DMenuParamRepository extends JpaRepository<DMenuParamEntity, UUID>, JpaSpecificationExecutor<DMenuParamEntity> {
    Optional<DMenuParamEntity> findByOid(UUID oid);
    boolean existsByOid(UUID oid);
    @Modifying
    void deleteByOid(UUID oid);

}
