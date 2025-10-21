package com.glt.connector.connector.linux.ubuntu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glt.connector.connection.Connection;
import com.glt.connector.connection.ConnectionFactory;
import com.glt.connector.connection.ssh.SshConnection;
import com.glt.connector.connector.ServerConnector;
import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.dto.GroupDto;
import com.glt.connector.model.Account;
import com.glt.connector.dto.ExecResult;
import com.glt.connector.model.Group;
import com.glt.connector.model.MachineInfo;
import com.glt.connector.model.Server;
import com.glt.connector.task.ScanTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component("UBUNTU")
@RequiredArgsConstructor
public class UbuntuConnector implements ServerConnector {

    String CMD_GET_USERS = """
    bash -c '
    echo "["
    first=1
    awk -F: '\\''$3 >= 1000 && $3 < 65534'\\'' /etc/passwd | while IFS=: read -r username x uid gid comment home shell; do
        [ "$username" = "nobody" ] && continue
        groups=$(id -nG "$username" 2>/dev/null | sed "s/ /\\",\\"/g")
        sid="LINUX-UID-$uid"
        active=true
        [[ "$shell" = "/usr/sbin/nologin" || "$shell" = "/bin/false" ]] && active=false
        [ $first -eq 0 ] && echo "," || first=0
        echo "  {"
        echo "    \\"login\\": \\"${username}\\","
        echo "    \\"uid\\": ${uid},"
        echo "    \\"homeDir\\": \\"/home/${username}\\","
        echo "    \\"shell\\": \\"${shell}\\","
        echo "    \\"description\\": \\"${comment}\\","
        echo "    \\"active\\": ${active},"
        echo "    \\"accountExpires\\": null,"
        echo "    \\"groups\\": [\\"${groups}\\"]"
        echo -n "  }"
    done
    echo
    echo "]"
    '
""";
    private static final String CMD_GET_CPU = "lscpu";
    private static final String CMD_GET_RAM = "free -h";
    private static final String CMD_GET_DISK = "df -h";
    private static final String CMD_GET_UPTIME = "uptime";

    @Qualifier("sshConnectionFactory")
    private final ConnectionFactory connectionFactory;

    @Override
    public List<AccountDto> scanUsers(ScanTask task) {
        try (Connection conn = connectionFactory.create(task)) {
            String output = conn.execStdout(CMD_GET_USERS);
            if (output == null || output.isBlank()) {
                throw new RuntimeException("Empty users output from " + task.getServerIp());
            }
            return parseUsers(output, task, conn);
        } catch (Exception e) {
            throw new RuntimeException("scanUsers failed on " + task.getServerIp() + ": " + e.getMessage(), e);
        }
    }


    @Override
    public Set<GroupDto> scanGroups(ScanTask task) {
        // Берём все поля: name:x:gid:user1,user2,...
        final String cmd = "getent group";
        ExecResult result = null;

        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(cmd);
            if (!result.isSuccess()) {
                throw new RuntimeException("Ошибка получения групп: " + result.getStderr());
            }

            String output = result.getStdout();
            if (output == null || output.isBlank()) {
                return Collections.emptySet();
            }

            Set<GroupDto> groups = new LinkedHashSet<>();
            ObjectMapper om = new ObjectMapper();

            for (String line : output.split("\\R")) {
                if (line == null || line.isBlank()) continue;

                // Формат: name:passwd:gid:members
                String[] parts = line.split(":", -1);
                String name = parts.length > 0 ? parts[0].trim() : null;
                String gidStr = parts.length > 2 ? parts[2].trim() : null;
                String membersStr = parts.length > 3 ? parts[3].trim() : "";

                if (name == null || name.isBlank()) continue;

                Integer gid = null;
                try { if (gidStr != null && !gidStr.isBlank()) gid = Integer.valueOf(gidStr); } catch (NumberFormatException ignored) {}

                List<String> members = new ArrayList<>();
                if (!membersStr.isBlank()) {
                    for (String m : membersStr.split(",")) {
                        if (!m.isBlank()) members.add(m.trim());
                    }
                }

                // Упакуем доп. данные в JSON
                String dataJson = om.writeValueAsString(Map.of(
                        "gid", gid,
                        "members", members
                ));

                groups.add(GroupDto.builder()
                        .group(name)
                        .serverId(task.getServerId())
                        .description(null)                 // в Linux, как правило, описания нет
                        .data(dataJson)
                        .lastSeen(LocalDateTime.now())     // выставляем явно, т.к. @Builder по умолчанию не подставит значение поля
                        .updatedAt(LocalDateTime.now())
                        .build());
            }

            log.info("На сервере {} найдено {} групп", task.getServerIp(), groups.size());
            return groups;

        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            log.error("Ошибка получения групп на {}: {}", task.getServerIp(), stderr, e);
            throw new RuntimeException("Ошибка получения групп: " + stderr, e);
        }
    }


    @Override
    public MachineInfo scanMachineInfo(ScanTask task) {
        try (Connection conn = connectionFactory.create(task)) {
            return new MachineInfo(
                    task.getServerIp(),
                    conn.execStdout(CMD_GET_CPU),
                    conn.execStdout(CMD_GET_RAM),
                    conn.execStdout(CMD_GET_DISK),
                    conn.execStdout(CMD_GET_UPTIME)
            );
        } catch (Exception e) {
            log.error("Ошибка при получении информации о машине {}: {}", task.getServerIp(), e.getMessage());
            return new MachineInfo(task.getServerIp(), "ERR", "ERR", "ERR", "ERR");
        }
    }

    @Override
    public void blockUser(ScanTask task, Account account) {
        String login = account.getLogin();
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login must be provided to block user");

        String cmd = "sudo usermod -s /usr/sbin/nologin " + login;

        ExecResult result = null;

        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(cmd);
            if (!result.isSuccess()) throw new RuntimeException("usermod failed or unsafe lock: " + result.getStderr());

            log.info("Пользователь {} заблокирован на сервере {}. Вывод: {}", login, task.getServerIp(), result.getStdout());
            account.setActive(false);
        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            throw new RuntimeException("Не удалось заблокировать пользователя " + login + ": " + stderr, e);
        }
    }


    @Override
    public void unblockUser(ScanTask task, Account account) {
        String login = account.getLogin();
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login must be provided to unblock user");

        String shell = account.getShell() != null ? account.getShell() : "/bin/bash";
        String cmd = "sudo usermod -s " + shell + " " + login;

        ExecResult result = null;

        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(cmd);
            if (!result.isSuccess() || result.getStderr().contains("passwordless account")) {
                throw new RuntimeException("usermod failed or unsafe unlock: " + result.getStderr());
            }
            log.info("Пользователь {} разблокирован на сервере {}. Вывод: {}", login, task.getServerIp(), result.getStdout());
            account.setActive(true);
        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            throw new RuntimeException("Не удалось разблокировать пользователя " + login + ": " + stderr, e);
        }
    }

    @Override
    public AccountDto createUser(ScanTask task, AccountDto accountDTO, Server server) {
        String login = accountDTO.getLogin();
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login must be provided to create user");

        StringBuilder cmd = new StringBuilder("sudo useradd -m");

        if (accountDTO.getShell() != null) cmd.append(" -s ").append(accountDTO.getShell());
        if (accountDTO.getDescription() != null) cmd.append(" -c '").append(accountDTO.getDescription().replace("'", "\\'")).append("'");
        if (accountDTO.getGroups() != null && !accountDTO.getGroups().isEmpty())
            cmd.append(" -G ").append(String.join(",", accountDTO.getGroups()));
        cmd.append(" ").append(login);
        ExecResult result = null;

        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(cmd.toString());
            if (!result.isSuccess() && !result.getStderr().contains("already exists")) {
                throw new RuntimeException("useradd failed: " + result.getStderr());
            }

            if (accountDTO.getPassword() != null) {
                String passwdCmd = String.format("echo '%s:%s' | sudo chpasswd", login, accountDTO.getPassword());
                ExecResult passwdResult = ((SshConnection) conn).execWithResult(passwdCmd);
                if (!passwdResult.isSuccess()) throw new RuntimeException("chpasswd failed: " + passwdResult.getStderr());
            }

            if (accountDTO.getAccountExpires() != null) {
                String chageCmd = String.format("sudo chage -E %s %s",
                        accountDTO.getAccountExpires().toLocalDate(), login);
                ExecResult chageResult = ((SshConnection) conn).execWithResult(chageCmd);
                if (!chageResult.isSuccess()) throw new RuntimeException("chage failed: " + chageResult.getStderr());
            }

            // Получаем данные
            String passwdLine = conn.execStdout("getent passwd " + login);
            String chageOutput = conn.execStdout("chage -l " + login);
            String idOutput = conn.execStdout("id -Gn " + login);

            if (passwdLine == null || passwdLine.isBlank()) throw new IllegalStateException("Пользователь не найден после создания");

            String[] parts = passwdLine.split(":");
            Integer uid = Integer.parseInt(parts[2]);
            String homeDir = parts[5];
            String shell = parts[6];
            String description = parts[4];
            Set<String> groups = Set.of(idOutput.trim().split("\\s+"));
            LocalDateTime expiresAt = parseExpireDate(chageOutput);

            return AccountDto.builder()
                    .login(login)
                    .serverId(server.getId())
                    .uid(uid)
                    .homeDir(homeDir)
                    .shell(shell)
                    .description(description)
                    .groups(groups)
                    .accountExpires(expiresAt)
                    .active(true)
                    .updatedAt(LocalDateTime.now())
                    .build();


        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            log.error("Ошибка при создании пользователя {} на {}: {}", login, task.getServerIp(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при создании пользователя " + login + ": " + stderr, e);
        }
    }

    private List<AccountDto> parseUsers(String rawOutput, ScanTask task, Connection connection) {
        List<AccountDto> users = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            List<Map<String, Object>> parsed = mapper.readValue(rawOutput, new TypeReference<>() {});
            for (Map<String, Object> userMap : parsed) {
                String login = (String) userMap.get("login");
                Integer uid = (Integer) userMap.get("uid");
                String homeDir = (String) userMap.get("homeDir");
                String shell = (String) userMap.get("shell");
                String description = (String) userMap.get("description");
                Boolean active = (Boolean) userMap.get("active");
                List<String> groupList = (List<String>) userMap.get("groups");

                LocalDateTime expires = fetchAccountExpiration(login, connection);
                String data = mapper.writeValueAsString(Map.of("shell", shell, "home", homeDir));

                users.add(AccountDto.builder()
                        .login(login)
                        .serverId(task.getServerId())
                        .uid(uid)
                        .homeDir(homeDir)
                        .shell(shell)
                        .description(description)
                        .data(data)
                        .accountExpires(expires)
                        .lastSeen(null)
                        .active(Boolean.TRUE.equals(active))
                        .updatedAt(LocalDateTime.now())
                        .groups(new HashSet<>(groupList))
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("parseUsers JSON failed: " + e.getMessage(), e);
        }

        return users;
    }

    private LocalDateTime fetchAccountExpiration(String login, Connection connection) {
        try {
            String chageOutput = connection.execStdout("chage -l " + login);
            return parseExpireDate(chageOutput);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseExpireDate(String chageOutput) {
        for (String line : chageOutput.lines().toList()) {
            if (line.toLowerCase().contains("account expires")) {
                String value = line.split(":")[1].trim();
                if (!value.equalsIgnoreCase("never")) {
                    try {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
                        return LocalDate.parse(value, fmt).atStartOfDay();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void addUserToGroup(ScanTask task, String login, String group) {
        String cmd = "sudo usermod -aG " + group + " " + login;
        ExecResult result = null;


        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(cmd);
            if (!result.isSuccess()) {
                throw new RuntimeException("Ошибка добавления в группу: " + result.getStderr());
            }
            log.info("Пользователь {} добавлен в группу {} на {}", login, group, task.getServerIp());
        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            log.error("Ошибка добавления {} в группу {}: {}", login, group, stderr);
            throw new RuntimeException("Ошибка добавления пользователя в группу: " + stderr, e);
        }
    }
    @Override
    public void removeUserFromGroup(ScanTask task, String login, String group) {
        String cmd = String.format("sudo gpasswd -d %s %s", login, group);

        ExecResult result = null;

        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(cmd);
            if (!result.isSuccess()) {
                throw new RuntimeException("Ошибка удаления из группы: " + result.getStderr());
            }
            log.info("Пользователь {} удалён из группы {} на {}", login, group, task.getServerIp());
        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            log.error("Ошибка удаления {} из группы {}: {}", login, group, stderr);
            throw new RuntimeException("Ошибка удаления пользователя из группы: " + stderr, e);
        }
    }

    @Override
    public void deleteUser(ScanTask task, Account account) {
        String login = account.getLogin();
        if (login == null || login.isBlank())
            throw new IllegalArgumentException("Login должен быть указан для удаления");

        String cmd = "sudo userdel -r " + login; // -r удаляет и домашнюю директорию

        ExecResult result = null;

        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(cmd);
            if (!result.isSuccess()) {
                throw new RuntimeException("Ошибка userdel: " + result.getStderr());
            }

            log.info("Пользователь {} удалён на сервере {}", login, task.getServerIp());
        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            throw new RuntimeException("Ошибка удаления пользователя " + login + ": " + stderr, e);
        }
    }

    @Override
    public void updateUser(ScanTask task, AccountDto accountDTO, Account existingAccount) {
        String login = existingAccount.getLogin();
        if (login == null || login.isBlank())
            throw new IllegalArgumentException("Login должен быть указан для обновления");

        try (Connection conn = connectionFactory.create(task)) {
            SshConnection ssh = (SshConnection) conn;

            if (accountDTO.getLogin() != null && !accountDTO.getLogin().equals(login)) {
                String cmd = "sudo usermod -l " + accountDTO.getLogin() + " " + login;
                ExecResult result = ssh.execWithResult(cmd);
                handleResult(result, cmd);
                login = accountDTO.getLogin();
            }

            // 2. Смена shell
            if (accountDTO.getShell() != null && !accountDTO.getShell().equals(existingAccount.getShell())) {
                String cmd = "sudo usermod -s " + accountDTO.getShell() + " " + login;
                ExecResult result = ssh.execWithResult(cmd);
                handleResult(result, cmd);
            }

            // 3. Смена описания
            if (accountDTO.getDescription() != null && !accountDTO.getDescription().equals(existingAccount.getDescription())) {
                String safeDescription = accountDTO.getDescription().replace("'", "\\'");
                String cmd = "sudo usermod -c '" + safeDescription + "' " + login;
                ExecResult result = ssh.execWithResult(cmd);
                handleResult(result, cmd);
            }

            // 4. Смена пароля
            if (accountDTO.getPassword() != null && !accountDTO.getPassword().isBlank()) {
                String cmd = String.format("echo '%s:%s' | sudo chpasswd", login, accountDTO.getPassword());
                ExecResult result = ssh.execWithResult(cmd);
                handleResult(result, cmd);
            }

            // 5. Смена срока действия
            if (accountDTO.getAccountExpires() != null &&
                    !Objects.equals(accountDTO.getAccountExpires(), existingAccount.getAccountExpires())) {
                String expireDate = accountDTO.getAccountExpires().toLocalDate().toString();
                String cmd = "sudo chage -E " + expireDate + " " + login;
                ExecResult result = ssh.execWithResult(cmd);
                handleResult(result, cmd);
            }

            log.info("Пользователь {} успешно обновлён на сервере {}", login, task.getServerIp());

        } catch (Exception e) {
            log.error("Ошибка при обновлении пользователя {}: {}", login, e.getMessage(), e);
            throw new RuntimeException("Ошибка при обновлении пользователя " + login + ": " + e.getMessage(), e);
        }
    }

    private void handleResult(ExecResult result, String cmd) {
        if (result == null) {
            throw new RuntimeException("Команда не вернула результата: " + cmd);
        }
        if (!result.isSuccess() || (result.getStderr() != null && !result.getStderr().isBlank())) {
            String msg = "Ошибка при выполнении команды: " + cmd + "\n" +
                    "stdout: " + result.getStdout() + "\n" +
                    "stderr: " + result.getStderr();
            throw new RuntimeException(msg);
        }
        log.debug("✅ Команда выполнена: {}\nstdout: {}\nstderr: {}", cmd, result.getStdout(), result.getStderr());
    }
    @Override
    public void changePassword(ScanTask task, Account account, String newPassword) {
        String login = account.getLogin();
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login должен быть указан для смены пароля");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Новый пароль не должен быть пустым");
        }

        ExecResult result = null;

        try (Connection conn = connectionFactory.create(task)) {
            // Экранируем спецсимволы для shell (особенно одинарные кавычки)
            String safeLogin = login.replace("'", "'\"'\"'");
            String safePassword = newPassword.replace("'", "'\"'\"'");

            // Формируем безопасную shell-команду
            String echoInput = safeLogin + ":" + safePassword;
            String passwdCmd = "echo '" + echoInput + "' | sudo chpasswd";
            log.info(echoInput);
            result = ((SshConnection) conn).execWithResult(passwdCmd);

            if (!result.isSuccess() || (result.getStderr() != null && !result.getStderr().isBlank())) {
                throw new RuntimeException("Ошибка смены пароля: " + result.getStderr());
            }

            log.info("Пароль пользователя {} успешно изменён на сервере {}", login, task.getServerIp());

        } catch (Exception e) {
            String stderr = result != null ? result.getStderr() : "UNKNOWN";
            throw new RuntimeException("Не удалось изменить пароль пользователя " + login + ": " + stderr, e);
        }
    }


}