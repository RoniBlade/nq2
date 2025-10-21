package org.example.repository.hibernate.view;

import org.example.entity.view.AccountAssociationViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountAssociationViewRepository extends JpaRepository<AccountAssociationViewEntity, Long> {

    Optional<AccountAssociationViewEntity> findByOid(UUID oid);
}
