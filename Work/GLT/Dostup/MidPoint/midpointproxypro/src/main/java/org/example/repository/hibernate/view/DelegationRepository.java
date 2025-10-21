package org.example.repository.hibernate.view;

import org.example.entity.view.DelegationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DelegationRepository extends JpaRepository<DelegationEntity, Long>, JpaSpecificationExecutor<DelegationEntity> {
}
