package com.glt.connector.connector.windows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glt.connector.connection.Connection;
import com.glt.connector.connection.ConnectionFactory;
import com.glt.connector.connection.ssh.SshConnection;
import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.dto.ExecResult;
import com.glt.connector.dto.GroupDto;
import com.glt.connector.model.Account;
import com.glt.connector.model.MachineInfo;
import com.glt.connector.connector.ServerConnector;
import com.glt.connector.model.Server;
import com.glt.connector.task.ScanTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("WINDOWS")
@RequiredArgsConstructor
public class WindowsConnector implements ServerConnector {

    private static final String PS_PREFIX =
            "$ErrorActionPreference='Stop'; $OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); ";

    private static final String CMD_GET_USERS_JSON =
            "powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command \"" +
                    "$ErrorActionPreference='Stop'; " +
                    "$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); " +
                    "$users = Get-LocalUser | Where-Object { [int](($_.SID.Value).Split('-')[-1]) -ge 1000 }; " +
                    "$result = foreach ($user in $users) { " +
                    "try { $groups = ([ADSI](\\\"WinNT://./$($user.Name),user\\\")).Groups() | ForEach-Object { $_.GetType().InvokeMember(\\\"Name\\\", 'GetProperty', $null, $_, $null) } } catch { $groups = @() }; " +
                    "[PSCustomObject]@{ " +
                    "Name = $user.Name; " +
                    "Description = $user.Description; " +
                    "Enabled = $user.Enabled; " +
                    "AccountExpires = $user.AccountExpires; " +
                    "SID = $user.SID.Value; " +
                    "Groups = $groups " +
                    "} }; " +
                    "$result | ConvertTo-Json -Compress\"";


    private static final String CMD_GET_CPU = "powershell -Command \"" + PS_PREFIX + "wmic cpu get name\"";
    private static final String CMD_GET_RAM = "powershell -Command \"" + PS_PREFIX + "wmic memorychip get capacity\"";
    private static final String CMD_GET_DISK = "powershell -Command \"" + PS_PREFIX + "wmic logicaldisk get size,freespace,caption\"";
    private static final String CMD_GET_UPTIME = "powershell -Command \"" + PS_PREFIX + "(Get-Date) - (Get-CimInstance Win32_OperatingSystem).LastBootUpTime\"";

    @Qualifier("sshConnectionFactory")
    private final ConnectionFactory connectionFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<AccountDto> scanUsers(ScanTask task) throws Exception {
        try (Connection connection = connectionFactory.create(task)) {
            String output = connection.execStdout(CMD_GET_USERS_JSON);
            if (output == null || output.isBlank()) {
                throw new RuntimeException("Empty users JSON from " + task.getServerIp());
            }
            return parseUsers(output, task);
        } catch (Exception e) {
            throw new RuntimeException("Windows scanUsers failed on " + task.getServerIp() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Set<GroupDto> scanGroups(ScanTask task) {
        final int MAX_RAW_CHARS = 1500;
        final int MAX_SAMPLES   = 10;

        String script = "powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command " +
                "\"$ErrorActionPreference='Stop'; " +
                "[Console]::InputEncoding=[System.Text.UTF8Encoding]::new($false); " +
                "[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false); " +
                "$groups=Get-LocalGroup|Select-Object Name,@{Name='SID';Expression={$_.SID.Value}},Description; " +
                "$groups|ConvertTo-Json -Compress\"";

        ExecResult result = null;
        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult((script));
            System.out.println("COMM " + (script));
            System.out.println("RESULT " + result.getStdout());
            if (!result.isSuccess()) {
                log.error("scanGroups: PowerShell failed on {}. Stderr: {}", task.getServerIp(), result.getStderr());
                throw new RuntimeException("Ошибка получения групп: " + result.getStderr());
            }

            String json = result.getStdout();
            if (json == null || json.isBlank()) {
                log.warn("scanGroups: Empty JSON from {}. stdout is blank.", task.getServerIp());
                return Collections.emptySet();
            }

            // ЛОГ №1 — превью сырого JSON
            String rawPreview = json.length() > MAX_RAW_CHARS ? json.substring(0, MAX_RAW_CHARS) + "…" : json;
            log.debug("scanGroups: RAW JSON from {} ({} chars): {}", task.getServerIp(), json.length(), rawPreview);

            Set<GroupDto> groups = new LinkedHashSet<>();
            JsonNode root = objectMapper.readTree(json);
            if (root.isObject()) {
                root = objectMapper.createArrayNode().add(root);
            }

            int totalNodes = 0;
            int missingSid = 0;
            int unparsableRid = 0;
            int builtinCount = 0;
            int domainBacked = 0;
            List<String> samples = new ArrayList<>();

            for (JsonNode node : root) {
                totalNodes++;

                String name = optTrim(node, "Name");
                if (name == null || name.isBlank()) continue;

                String description = optTrim(node, "Description");
                String sid = optTrim(node, "SID");

                if (sid == null || sid.isBlank()) {
                    missingSid++;
                }

                Integer rid = null;
                if (sid != null && !sid.isBlank()) {
                    try {
                        String[] parts = sid.split("-");
                        rid = Integer.parseInt(parts[parts.length - 1]);
                    } catch (Exception ex) {
                        unparsableRid++;
                    }
                }

                if (sid != null && sid.startsWith("S-1-5-32-")) {
                    builtinCount++;
                }

                String dataJson = objectMapper.writeValueAsString(Map.of(
                        "sid", sid,
                        "rid", rid
                ));

                groups.add(GroupDto.builder()
                        .group(name)
                        .serverId(task.getServerId())
                        .description(description)
                        .data(dataJson)
                        .lastSeen(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

                if (samples.size() < MAX_SAMPLES) {
                    samples.add(String.format("{name=\"%s\", sid=\"%s\", rid=%s}", name, sid, String.valueOf(rid)));
                }
            }

            log.debug("scanGroups: {} → parsed {} groups (nodes seen: {}, builtin:{}, missingSid:{}, badRid:{})",
                    task.getServerIp(), groups.size(), totalNodes, builtinCount, missingSid, unparsableRid);

            if (!samples.isEmpty()) {
                log.debug("scanGroups: parsed samples on {} (first {}): {}", task.getServerIp(), samples.size(), samples);
            }

            return groups;

        } catch (Exception e) {
            String stderr = (result != null ? result.getStderr() : "UNKNOWN");
            log.error("scanGroups: exception on {}. stderr: {}", task.getServerIp(), stderr, e);
            throw new RuntimeException("Ошибка получения всех групп: " + stderr, e);
        }
    }

//    @Override
//    public Set<GroupDto> scanGroups(ScanTask task) throws Exception {
//
//        String getGroupsScript = "gcim Win32_Account | ft Name";
//
//        try(Connection connection = connectionFactory.create(task)) {
//            String output = connection.execStdout(getGroupsScript);
//            return parseGroups(output, task);
//        }
//
//
//    }
//
//    private Set<GroupDto> parseGroups(String output, ScanTask task) {
//
//        Set<String> names = Arrays.stream(output.split("\\R"))
//                .map(String::trim)
//                .filter(s -> !s.isEmpty())
//                .filter(s -> !s.equalsIgnoreCase("Name"))
//                .filter(s -> !s.equals("----"))
//                .collect(Collectors.toSet());
//
//        names.forEach(System.out::println);
//
//
//
//
//    }


    /** безопасно достаёт и триммит строку из JsonNode */
    private static String optTrim(JsonNode node, String field) {
        if (node == null || field == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return (s != null) ? s.trim() : null;
    }

    private List<AccountDto> parseUsers(String json, ScanTask task) {
        List<AccountDto> users = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.isObject()) {
                root = objectMapper.createArrayNode().add(root);
            }
            for (JsonNode node : root) {
                String login = node.path("Name").asText();
                String comment = node.path("Description").asText(null);
                boolean active = node.path("Enabled").asBoolean(true);
                String sid = node.path("SID").asText();
                int uid = 0;
                try {
                    String[] parts = sid.split("-");
                    uid = Integer.parseInt(parts[parts.length - 1]);
                } catch (Exception e) {
                    log.warn("Не удалось извлечь UID из SID '{}': {}", sid, e.getMessage());
                }
                LocalDateTime accountExpires = null;
                if (node.has("AccountExpires") && !node.get("AccountExpires").isNull()) {
                    try {
                        long millis = objectMapper.convertValue(node.get("AccountExpires"), Long.class);
                        accountExpires = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                    } catch (Exception e) {
                        log.warn("Не удалось распарсить дату истечения для {}: {}", login, e.getMessage());
                    }
                }
                Set<String> groups = new HashSet<>();
                JsonNode groupsNode = node.path("Groups");
                if (groupsNode.isArray()) {
                    for (JsonNode g : groupsNode) {
                        String groupName = g.asText();
                        if (groupName == null || groupName.isBlank()) continue;
                        groups.add(groupName);
                    }
                }
                users.add(AccountDto.builder()
                        .login(login)
                        .serverId(task.getServerId())
                        .uid(uid)
                        .homeDir(null)
                        .shell("powershell")
                        .description(comment)
                        .data("")
                        .accountExpires(accountExpires)
                        .lastSeen(null)
                        .active(active)
                        .updatedAt(LocalDateTime.now())
                        .groups(groups)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("parseUsers(JSON) failed: " + e.getMessage(), e);
        }
        return users;
    }

    @Override
    public MachineInfo scanMachineInfo(ScanTask task) {
        try (Connection connection = connectionFactory.create(task)) {
            String cpu = connection.execStdout(CMD_GET_CPU);
            String ram = connection.execStdout(CMD_GET_RAM);
            String disk = connection.execStdout(CMD_GET_DISK);
            String uptime = connection.execStdout(CMD_GET_UPTIME);
            return new MachineInfo(task.getServerIp(), cpu, ram, disk, uptime);
        } catch (Exception e) {
            log.error("Ошибка при получении информации о машине {}: {}", task.getServerIp(), e.getMessage());
            return null;
        }
    }

    @Override
    public void blockUser(ScanTask task, Account account) {
        runSimplePs(task, String.format("Disable-LocalUser -Name '%s'", account.getLogin()), "заблокировать");
        account.setActive(false);
    }

    @Override
    public void unblockUser(ScanTask task, Account account) {
        runSimplePs(task, String.format("Enable-LocalUser -Name '%s'", account.getLogin()), "разблокировать");
        account.setActive(true);
    }

    @Override
    public AccountDto createUser(ScanTask task, AccountDto accountDTO, Server server) {
        String login = accountDTO.getLogin();
        String password = accountDTO.getPassword();
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login must be provided to create user");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password must be provided to create user");

        List<String> errors = new ArrayList<>();
        Set<String> successfulGroups = new HashSet<>();
        try (Connection conn = connectionFactory.create(task)) {
            SshConnection sshConn = (SshConnection) conn;
            ExecResult userResult = sshConn.execWithResult(psCmd(String.format("net user \"%s\" \"%s\" /add", login, password)));
            if (!userResult.isSuccess()) throw new RuntimeException("Ошибка создания пользователя: " + userResult.getStderr());
            if (accountDTO.getGroups() != null) {
                for (String group : accountDTO.getGroups()) {
                    var groupResult = sshConn.execWithResult(psCmd(String.format("net localgroup \"%s\" \"%s\" /add", group.trim(), login)));
                    if (!groupResult.isSuccess()) {
                        errors.add("Ошибка добавления в группу " + group + ": " + groupResult.getStderr());
                    } else {
                        successfulGroups.add(group);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании пользователя: " + e.getMessage(), e);
        }
        return AccountDto.builder()
                .login(login)
                .serverId(task.getServerId())
                .description(accountDTO.getDescription())
                .accountExpires(accountDTO.getAccountExpires())
                .shell("cmd")
                .active(accountDTO.getActive() != null ? accountDTO.getActive() : true)
                .updatedAt(LocalDateTime.now())
                .data(errors.isEmpty() ? "" : String.join("; ", errors))
                .groups(successfulGroups)
                .build();
    }

    @Override
    public void addUserToGroup(ScanTask task, String login, String group) {
        runSimplePs(task, String.format("net localgroup \"%s\" \"%s\" /add", group, login), "добавить в группу");
    }

    @Override
    public void removeUserFromGroup(ScanTask task, String login, String group) {
        runSimplePs(task, String.format("net localgroup \"%s\" \"%s\" /delete", group, login), "удалить из группы");
    }

    @Override
    public void deleteUser(ScanTask task, Account account) {
        runSimplePs(task, String.format("net user \"%s\" /delete", account.getLogin()), "удалить пользователя");
    }

    @Override
    public void changePassword(ScanTask task, Account account, String newPassword) {
        runSimplePs(task, String.format("net user \"%s\" \"%s\"", account.getLogin(), newPassword), "сменить пароль");
    }

    @Override
    public void updateUser(ScanTask task, AccountDto accountDTO, Account existingAccount) {
        try (Connection conn = connectionFactory.create(task)) {
            SshConnection ssh = (SshConnection) conn;
            String login = existingAccount.getLogin();
            if (accountDTO.getLogin() != null && !accountDTO.getLogin().equals(login)) {
                handleResult(ssh.execWithResult(psCmd(String.format("Rename-LocalUser -Name '%s' -NewName '%s'", login, accountDTO.getLogin()))),
                        "Rename-LocalUser");
                login = accountDTO.getLogin();
            }
            if (accountDTO.getDescription() != null && !accountDTO.getDescription().equals(existingAccount.getDescription())) {
                handleResult(ssh.execWithResult(psCmd(String.format("Set-LocalUser -Name '%s' -Description '%s'", login, accountDTO.getDescription()))),
                        "Set-LocalUser Description");
            }
            if (accountDTO.getPassword() != null && !accountDTO.getPassword().isBlank()) {
                handleResult(ssh.execWithResult(psCmd(
                        String.format("$p = ConvertTo-SecureString '%s' -AsPlainText -Force; Set-LocalUser -Name '%s' -Password $p",
                                accountDTO.getPassword(), login))), "Set-LocalUser Password");
            }
            if (accountDTO.getAccountExpires() != null &&
                    !Objects.equals(accountDTO.getAccountExpires(), existingAccount.getAccountExpires())) {
                String expireDate = accountDTO.getAccountExpires().toLocalDate().toString();
                handleResult(ssh.execWithResult(psCmd(
                        String.format("$d=[datetime]::ParseExact('%s','yyyy-MM-dd',$null); Set-LocalUser -Name '%s' -AccountExpires $d",
                                expireDate, login))), "Set-LocalUser AccountExpires");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при обновлении пользователя: " + e.getMessage(), e);
        }
    }

    private String psCmd(String script) {
        return "powershell -Command \"" + PS_PREFIX + script + "\"";
    }

    private void runSimplePs(ScanTask task, String script, String action) {
        ExecResult result;
        try (Connection conn = connectionFactory.create(task)) {
            result = ((SshConnection) conn).execWithResult(psCmd(script));
            if (!result.isSuccess()) throw new RuntimeException("Не удалось " + action + ": " + result.getStderr());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении действия (" + action + "): " + e.getMessage(), e);
        }
    }

    private void handleResult(ExecResult result, String cmd) {
        if (result == null || !result.isSuccess() || (result.getStderr() != null && !result.getStderr().isBlank())) {
            throw new RuntimeException("Ошибка при выполнении команды: " + cmd + "\nstdout: " +
                    (result != null ? result.getStdout() : "") + "\nstderr: " +
                    (result != null ? result.getStderr() : ""));
        }
    }
}
