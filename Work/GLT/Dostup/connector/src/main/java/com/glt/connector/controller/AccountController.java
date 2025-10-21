package com.glt.connector.controller;

import com.glt.connector.connector.ServerConnector;
import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.dto.GroupDto;
import com.glt.connector.model.Account;
import com.glt.connector.model.Group;
import com.glt.connector.model.Server;
import com.glt.connector.model.enums.OperatingSystemType;
import com.glt.connector.repository.AccountRepository;
import com.glt.connector.repository.GroupRepository;
import com.glt.connector.repository.ServerRepository;
import com.glt.connector.service.AccountService;
import com.glt.connector.service.ServerScannerRegistry;
import com.glt.connector.task.ScanTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final ServerRepository serverRepository;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final GroupRepository groupRepository;
    private final ServerScannerRegistry serverScannerRegistry;

    @GetMapping("/servers/{serverId}/scan")
    public ResponseEntity<?> scanServer(@PathVariable Long serverId) {
        Optional<Server> optionalServer = serverRepository.findById(serverId);

        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body("Сервер не найден: " + serverId);
        }

        Server server = optionalServer.get();
        OperatingSystemType osType = server.getOsType();
        ServerConnector connector = serverScannerRegistry.getScanner(osType);

        if (connector == null) {
            return ResponseEntity.badRequest().body("Неизвестная ОС: " + osType);
        }

        ScanTask task = ScanTask.builder()
                .serverId(server.getId())
                .serverIp(server.getHost())
                .port(server.getPort())
                .login(server.getLogin())
                .encryptedPassword(server.getPassword())
                .osType(osType)
                .build();

        try {
            // 1) Сканируем пользователей и группы
            List<AccountDto> users = connector.scanUsers(task);
            Set<GroupDto> groups  = connector.scanGroups(task);

            // 2) Сохраняем группы (если есть)
            if (groups != null && !groups.isEmpty()) {
                accountService.upsertGroups(groups);
            }

            int groupsCount = (groups != null ? groups.size() : 0); // <- фикс возможного NPE в логе
            log.info("На сервере {} найдено {} групп", task.getServerIp(), groupsCount);

            // 3) Сохраняем пользователей и их связи
            if (users != null && !users.isEmpty()) {
                accountService.upsertBatch(users);
            }

            // 4) Возвращаем обе коллекции
            Map<String, Object> body = new HashMap<>();
            body.put("usersCount", users != null ? users.size() : 0);
            body.put("groupsCount", groupsCount);
            body.put("users", users);
            body.put("groups", groups);

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("Ошибка при сканировании сервера {}: {}", server.getHost(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при сканировании: " + e.getMessage());
        }
    }

    @PostMapping("/accounts/{serverId}/create")
    public ResponseEntity<?> createAccount(
            @PathVariable Long serverId,
            @RequestBody AccountDto dto
    ) {
        Optional<Server> optionalServer = serverRepository.findById(serverId);
        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body("Сервер не найден: " + serverId);
        }

        Server server = optionalServer.get();
        OperatingSystemType osType = server.getOsType();

        ServerConnector connector = serverScannerRegistry.getScanner(osType);
        if (connector == null) {
            return ResponseEntity.badRequest().body("Неизвестная ОС: " + server.getOsType());
        }

        ScanTask task = ScanTask.builder()
                .serverId(server.getId())
                .serverIp(server.getHost())
                .port(server.getPort())
                .login(server.getLogin())
                .encryptedPassword(server.getPassword())
                .osType(server.getOsType())
                .build();

        try {
            AccountDto created = connector.createUser(task, dto, server);
            accountService.upsertBatch(List.of(created));

            Optional<Account> saved = accountRepository.findByLoginAndServerId(created.getLogin(), serverId);
            if (saved.isPresent()) {
                return ResponseEntity.ok(saved.get());
            } else {
                return ResponseEntity.status(500).body("Пользователь создан, но не найден в базе");
            }
        } catch (Exception e) {
            log.error("Ошибка при создании пользователя {}: {}", dto.getLogin(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при создании: " + e.getMessage());
        }
    }

    @PutMapping("/accounts/{serverId}/update/{accountId}")
    public ResponseEntity<?> updateAccount(
            @PathVariable Long serverId,
            @PathVariable Long accountId,
            @RequestBody AccountDto dto
    ) {
        log.info("[UPDATE] accountId={} serverId={} DTO: {}", accountId, serverId, dto);

        Optional<Server> optionalServer = serverRepository.findById(serverId);
        Optional<Account> optionalAccount = accountRepository.findById(accountId);

        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body("Сервер не найден: " + serverId);
        }
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.badRequest().body("Пользователь не найден: " + accountId);
        }

        Server server = optionalServer.get();
        Account account = optionalAccount.get();
        ServerConnector connector = serverScannerRegistry.getScanner(server.getOsType());

        if (connector == null) {
            return ResponseEntity.badRequest().body("Неизвестный тип ОС: " + server.getOsType());
        }

        ScanTask task = ScanTask.builder()
                .serverId(server.getId())
                .serverIp(server.getHost())
                .port(server.getPort())
                .login(server.getLogin())
                .encryptedPassword(server.getPassword())
                .osType(server.getOsType())
                .build();

        try {
            connector.updateUser(task, dto, account);

            if (dto.getLogin() == null) dto.setLogin(account.getLogin());
            if (dto.getServerId() == null) dto.setServerId(serverId);
            dto.setUpdatedAt(LocalDateTime.now());

            log.info("[UPDATE] DTO для updateById: {}", dto);

            accountService.updateById(dto, accountId);

            log.info("[UPDATE] Обновление завершено успешно");
            return ResponseEntity.ok("Пользователь обновлён");
        } catch (Exception e) {
            log.error("[UPDATE] Ошибка при обновлении пользователя {}: {}", account.getLogin(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при обновлении: " + e.getMessage());
        }
    }

    @DeleteMapping("/accounts/{serverId}/delete/{accountId}")
    public ResponseEntity<?> deleteAccount(
            @PathVariable Long serverId,
            @PathVariable Long accountId
    ) {
        Optional<Server> optionalServer = serverRepository.findById(serverId);
        Optional<Account> optionalAccount = accountRepository.findById(accountId);

        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body("Сервер не найден: " + serverId);
        }
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.badRequest().body("Пользователь не найден: " + accountId);
        }

        Server server = optionalServer.get();
        Account account = optionalAccount.get();

        ServerConnector connector = serverScannerRegistry.getScanner(server.getOsType());
        if (connector == null) {
            return ResponseEntity.badRequest().body("Неизвестная ОС: " + server.getOsType());
        }

        ScanTask task = ScanTask.builder()
                .serverId(server.getId())
                .serverIp(server.getHost())
                .port(server.getPort())
                .login(server.getLogin())
                .encryptedPassword(server.getPassword())
                .osType(server.getOsType())
                .build();

        try {
            connector.deleteUser(task, account);
            accountService.deleteById(accountId);
            return ResponseEntity.ok("Пользователь успешно удалён");
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя {}: {}", account.getLogin(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при удалении: " + e.getMessage());
        }
    }

    @PutMapping("/accounts/{serverId}/block/{userId}")
    public ResponseEntity<?> blockAccount(
            @PathVariable Long serverId,
            @PathVariable Long userId
    ) {
        Optional<Server> optionalServer = serverRepository.findById(serverId);
        Optional<Account> optionalAccount = accountRepository.findById(userId);

        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body("Сервер с id " + serverId + " не найден");
        }
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.badRequest().body("Пользователь с id " + userId + " не найден");
        }

        Server server = optionalServer.get();
        Account account = optionalAccount.get();

        String osType = server.getOsType().toString();
        ServerConnector connector = serverScannerRegistry.getScanner(OperatingSystemType.valueOf(osType));
        if (connector == null) {
            return ResponseEntity.badRequest().body("Неизвестный тип ОС: " + osType);
        }

        ScanTask task = ScanTask.builder()
                .serverId(server.getId())
                .serverIp(server.getHost())
                .port(server.getPort())
                .login(server.getLogin())
                .encryptedPassword(server.getPassword())
                .osType(server.getOsType())
                .build();

        try {
            connector.blockUser(task, account);
            accountRepository.save(account);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Ошибка при блокировке пользователя {}: {}", account.getLogin(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при блокировке: " + e.getMessage());
        }
    }

    @PutMapping("/accounts/{serverId}/unblock/{userId}")
    public ResponseEntity<?> unblockAccount(
            @PathVariable Long serverId,
            @PathVariable Long userId
    ) {
        Optional<Server> optionalServer = serverRepository.findById(serverId);
        Optional<Account> optionalAccount = accountRepository.findById(userId);

        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body("Сервер с id " + serverId + " не найден");
        }
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.badRequest().body("Пользователь с id " + userId + " не найден");
        }

        Server server = optionalServer.get();
        Account account = optionalAccount.get();

        OperatingSystemType osType = server.getOsType();
        ServerConnector connector = serverScannerRegistry.getScanner(osType);
        if (connector == null) {
            return ResponseEntity.badRequest().body("Неизвестный тип ОС: " + osType);
        }

        ScanTask task = ScanTask.builder()
                .serverId(server.getId())
                .serverIp(server.getHost())
                .port(server.getPort())
                .login(server.getLogin())
                .encryptedPassword(server.getPassword())
                .osType(server.getOsType())
                .build();

        try {
            connector.unblockUser(task, account);
            accountRepository.save(account);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Ошибка при разблокировке пользователя {}: {}", account.getLogin(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при разблокировке: " + e.getMessage() +
                    (e.getCause() != null ? " | " + e.getCause().getMessage() : ""));
        }
    }

    @PostMapping("/accounts/{groupId}/add/{accountId}")
    public ResponseEntity<?> addAccountToGroup(
            @PathVariable Long groupId,
            @PathVariable Long accountId
    ) {
        try {
            // Берём аккаунт сразу с server (join fetch), чтобы не было LazyInitializationException
            Account account = accountRepository.findByIdWithServer(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

            Server server = account.getServer(); // уже инициализирован

            ScanTask task = ScanTask.builder()
                    .serverId(server.getId())
                    .serverIp(server.getHost())
                    .port(server.getPort())
                    .login(server.getLogin())
                    .encryptedPassword(server.getPassword())
                    .osType(server.getOsType())
                    .build();

            ServerConnector connector = serverScannerRegistry.getScanner(server.getOsType());
            connector.addUserToGroup(task, account.getLogin(), group.getGroup());

            accountService.addUserToGroup(accountId, groupId);

            return ResponseEntity.ok("Пользователь добавлен в группу");
        } catch (Exception e) {
            log.error("Ошибка при добавлении accountId={} в groupId={}: {}", accountId, groupId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка: " + e.getMessage());
        }
    }

    @DeleteMapping("/accounts/{groupId}/remove/{accountId}")
    public ResponseEntity<?> removeAccountFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long accountId
    ) {
        try {
            // Берём аккаунт сразу с server (join fetch), чтобы не было LazyInitializationException
            Account account = accountRepository.findByIdWithServer(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

            Server server = account.getServer(); // уже инициализирован

            ScanTask task = ScanTask.builder()
                    .serverId(server.getId())
                    .serverIp(server.getHost())
                    .port(server.getPort())
                    .login(server.getLogin())
                    .encryptedPassword(server.getPassword())
                    .osType(server.getOsType())
                    .build();

            ServerConnector connector = serverScannerRegistry.getScanner(server.getOsType());

            log.info(group.getGroup());
            connector.removeUserFromGroup(task, account.getLogin(), group.getGroup());

            accountService.removeUserFromGroup(accountId, groupId);

            return ResponseEntity.ok("Пользователь удалён из группы");
        } catch (Exception e) {
            log.error("Ошибка при удалении accountId={} из groupId={}: {}", accountId, groupId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка: " + e.getMessage());
        }
    }

    @PutMapping("/accounts/{serverId}/password/{accountId}")
    public ResponseEntity<?> changePassword(
            @PathVariable Long serverId,
            @PathVariable Long accountId,
            @RequestBody String newPassword
    ) {
        Optional<Server> optionalServer = serverRepository.findById(serverId);
        Optional<Account> optionalAccount = accountRepository.findById(accountId);

        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body("Сервер не найден: " + serverId);
        }

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.badRequest().body("Пользователь не найден: " + accountId);
        }

        Server server = optionalServer.get();
        Account account = optionalAccount.get();
        ServerConnector connector = serverScannerRegistry.getScanner(server.getOsType());

        if (connector == null) {
            return ResponseEntity.badRequest().body("Неизвестный тип ОС: " + server.getOsType());
        }

        ScanTask task = ScanTask.builder()
                .serverId(server.getId())
                .serverIp(server.getHost())
                .port(server.getPort())
                .login(server.getLogin())
                .encryptedPassword(server.getPassword())
                .osType(server.getOsType())
                .build();

        try {
            connector.changePassword(task, account, newPassword);
            return ResponseEntity.ok("Пароль успешно изменён");
        } catch (Exception e) {
            log.error("Ошибка при смене пароля пользователя {}: {}", account.getLogin(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при смене пароля: " + e.getMessage());
        }
    }
}
