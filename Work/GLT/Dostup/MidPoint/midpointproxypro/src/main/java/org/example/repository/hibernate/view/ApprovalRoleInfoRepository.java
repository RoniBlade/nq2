package org.example.repository.hibernate.view;

import org.example.entity.view.ApprovalRoleInfoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRoleInfoRepository extends JpaRepository<ApprovalRoleInfoEntity, Long>,
        JpaSpecificationExecutor<ApprovalRoleInfoEntity> {

}
