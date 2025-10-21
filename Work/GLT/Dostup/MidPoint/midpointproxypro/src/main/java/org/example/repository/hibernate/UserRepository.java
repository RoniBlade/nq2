package org.example.repository.hibernate;

import org.example.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Blob;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>{

//    @Transactional
//    @Modifying
//    @Query(value = "UPDATE m_user SET photo = ?1 WHERE oid = ?2", nativeQuery = true)
//    void updatePhotoByOid(byte[] photo, UUID oid);

//    @Transactional
//    @Modifying
//    @Query("UPDATE UserEntity u SET u.photo = :photo WHERE u.oid = :oid")
//    void updatePhotoByOid(@Param("photo") byte[] photo, @Param("oid") UUID oid);

    @Transactional
    @Modifying
    @Query(value = "UPDATE m_user SET photo = CAST(?1 AS bytea) WHERE oid = ?2", nativeQuery = true)
    void updatePhotoByOid(byte[] photo, UUID oid);


    @Query(value = "SELECT photo FROM m_user WHERE oid = :oid", nativeQuery = true)
    byte[] getPhotoByOid(@Param("oid") UUID oid);

    @Transactional
    @Modifying
    @Query(value = "UPDATE m_user SET photo = NULL WHERE oid = :oid", nativeQuery = true)
    void deletePhotoByOid(@Param("oid") UUID oid);
}
