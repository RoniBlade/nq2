package org.example.repository.hibernate.view;

import org.example.entity.view.UserAssignmentsGrafEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserAssignmentsGrafRepository
        extends JpaRepository<UserAssignmentsGrafEntity, Long>,
        JpaSpecificationExecutor<UserAssignmentsGrafEntity> {
}
