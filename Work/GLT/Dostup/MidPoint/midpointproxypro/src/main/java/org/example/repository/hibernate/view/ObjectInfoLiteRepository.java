package org.example.repository.hibernate.view;

import org.example.entity.view.ObjectInfoLiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectInfoLiteRepository extends
        JpaRepository<ObjectInfoLiteEntity, Long>,
        JpaSpecificationExecutor<ObjectInfoLiteEntity> {
}
