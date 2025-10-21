package org.example.repository.hibernate.view;

import org.example.entity.view.MyRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MyRequestRepository extends JpaRepository<MyRequestEntity, Long>, JpaSpecificationExecutor<MyRequestEntity> {
    // Поиск заявок по OID цели (targetOid) с поддержкой пагинации
    Page<MyRequestEntity> findByTargetOid(UUID targetOid, Pageable pageable);
}
