package com.glt.connector.repository;

import com.glt.connector.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g WHERE g.group = :group AND g.server.id = :serverId")
    Optional<Group> findByNameAndServerId(@Param("group") String group, @Param("serverId") Long serverId);
}
