package org.example.repository.hibernate;

import org.example.entity.AccessCertCampaignEntity;
import org.example.entity.AccessCertDefinitionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccessCertDefinitionRepository extends
        JpaRepository<AccessCertDefinitionEntity, UUID>,
        JpaSpecificationExecutor<AccessCertDefinitionEntity> {

    @Query(value = "SELECT * FROM m_access_cert_definition WHERE oid = :oid", nativeQuery = true)
    Page<AccessCertDefinitionEntity> getByOid(@Param("oid") UUID oid, Pageable pageable);
}
