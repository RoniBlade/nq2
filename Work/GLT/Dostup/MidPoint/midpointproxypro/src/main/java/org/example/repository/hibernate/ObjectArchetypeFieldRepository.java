package org.example.repository.hibernate;

import org.example.entity.ObjectArchetypeFieldEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectArchetypeFieldRepository extends
        JpaRepository<ObjectArchetypeFieldEntity, Long>,
        JpaSpecificationExecutor<ObjectArchetypeFieldEntity> {

    List<ObjectArchetypeFieldEntity> findByExtArchetypeAndObjectTypeAndSend(String archetype, String fieldName, Boolean send);

    Optional<ObjectArchetypeFieldEntity> findByExtArchetypeAndFieldName(String archetype, String fieldName);
    Optional<ObjectArchetypeFieldEntity> findByObjectTypeAndFieldName(String archetype, String fieldName);

    Page<ObjectArchetypeFieldEntity> findByExtArchetypeAndSend(String archetype, boolean send, Pageable pageable);

    List<ObjectArchetypeFieldEntity> findByExtArchetypeAndSend(String archetype, boolean b);

}

