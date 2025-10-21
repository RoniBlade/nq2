package com.glt.connector.repository;

import com.glt.connector.model.Account;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByLoginAndServerId(String login, Long serverId);

    // Подтягиваем сервер вместе с аккаунтом (join fetch)
    @Query("select a from Account a join fetch a.server where a.id = :id")
    Optional<Account> findByIdWithServer(@Param("id") Long id);

    // Если где-то нужно искать по логину/серверу и сразу с сервером
    @Query("select a from Account a join fetch a.server where a.login = :login and a.serverId = :serverId")
    Optional<Account> findByLoginAndServerIdWithServer(@Param("login") String login,
                                                       @Param("serverId") Long serverId);
}
