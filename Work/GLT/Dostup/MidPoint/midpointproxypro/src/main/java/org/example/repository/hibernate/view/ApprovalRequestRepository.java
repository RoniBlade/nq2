package org.example.repository.hibernate.view;

import org.example.entity.view.ApprovalRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, Long>, JpaSpecificationExecutor<ApprovalRequestEntity> {


}
