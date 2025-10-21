package org.example.v1.repository;

import org.example.v1.entity.ObjectTypeFieldEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObjectTypeFieldRepository extends
        JpaRepository<ObjectTypeFieldEntity, UUID>,
        JpaSpecificationExecutor<ObjectTypeFieldEntity> {
    List<ObjectTypeFieldEntity> findByObjecttype(String objectType);
    List<ObjectTypeFieldEntity> findByObjecttypeAndSend(String objectType, Boolean send);

    List<ObjectTypeFieldEntity> findByObjecttypeAndSend(String objectType, boolean send, Sort sort);

    ObjectTypeFieldEntity findByFieldnameAndArchetypeAndSendTrue(String fieldName, String archetype);

    List<ObjectTypeFieldEntity> findByObjecttypeAndArchetypeAndSend(
            String objectType, String extArchetype, boolean send, Sort sort
    );
    boolean existsByOid(UUID oid);
    @Modifying
    void deleteByOid(UUID oid);


    @Query("select distinct o.objecttype from ObjectTypeFieldEntity o where o.objecttype is not null")
    List<String> findDistinctObjectTypes();

    @Query("select distinct o.archetype from ObjectTypeFieldEntity o where o.objecttype = :objectType and o.archetype is not null")
    List<String> findDistinctArchetypesByObjectType(@Param("objectType") String objectType);
}