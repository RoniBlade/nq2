package org.example.repository.hibernate.view;

import org.example.entity.view.AccountAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountAttributeRepository
        extends JpaRepository<AccountAttributeEntity, Long>,
        JpaSpecificationExecutor<AccountAttributeEntity> {
}
