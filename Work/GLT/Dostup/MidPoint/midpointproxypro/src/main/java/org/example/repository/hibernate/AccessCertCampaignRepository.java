package org.example.repository.hibernate;

import org.example.entity.AccessCertCampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccessCertCampaignRepository extends
        JpaRepository<AccessCertCampaignEntity, UUID>,
        JpaSpecificationExecutor<AccessCertCampaignEntity> {

    @Query(value = "SELECT * FROM m_access_cert_campaign WHERE oid = :oid", nativeQuery = true)
    Page<AccessCertCampaignEntity> getByOid(@Param("oid") UUID oid, Pageable pageable);

}
