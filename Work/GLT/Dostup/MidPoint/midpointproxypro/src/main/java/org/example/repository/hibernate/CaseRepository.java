package org.example.repository.hibernate;

import org.example.entity.CaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, UUID>, JpaSpecificationExecutor<CaseEntity> {

    @Query(value = "SELECT * FROM m_case WHERE nameOrig = ':nameOrig'", nativeQuery = true)
    Page<CaseEntity> getCaseByName(Pageable pageable, @Param("nameOrig") String nameOrig);

    @Query(value = "SELECT * FROM m_case WHERE oid = :oid", nativeQuery = true)
    Page<CaseEntity> getCaseByOid(Pageable pageable, @Param("oid") UUID oid);

}
