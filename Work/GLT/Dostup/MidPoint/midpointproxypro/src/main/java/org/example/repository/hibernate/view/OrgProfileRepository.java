package org.example.repository.hibernate.view;

import org.example.entity.view.OrgProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgProfileRepository extends
        JpaRepository<OrgProfileEntity, Long>,
        JpaSpecificationExecutor<OrgProfileEntity> {

}
