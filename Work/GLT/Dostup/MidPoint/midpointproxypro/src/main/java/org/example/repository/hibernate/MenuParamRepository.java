package org.example.repository.hibernate;

import org.example.entity.view.MenuParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuParamRepository extends JpaRepository<MenuParamEntity, Integer>, JpaSpecificationExecutor<MenuParamEntity> {
    Optional<MenuParamEntity> findByOid(String oid);
}
