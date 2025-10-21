package org.example.repository.hibernate.view;

import org.example.entity.view.CaseInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseInfoRepository extends JpaRepository<CaseInfoEntity, Long>, JpaSpecificationExecutor<CaseInfoEntity> {

}
