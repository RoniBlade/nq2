package org.example.repository.hibernate.view;

import org.example.entity.view.AssociationObjectViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssociationObjectViewRepository extends JpaRepository<AssociationObjectViewEntity, Long> {

    @Query("SELECT a FROM AssociationObjectViewEntity a WHERE a.oid IN :oids")
    List<AssociationObjectViewEntity> findAllByOidIn(@Param("oids") List<UUID> oids);
}

