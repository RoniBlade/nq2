package com.glt.connector.service;

import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.dto.GroupDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final JdbcTemplate jdbcTemplate;
    private final PerfLogger perf;

    /* ======================= ПУБЛИЧНЫЕ ОПЕРАЦИИ ======================= */

    /**
     * Сохраняет батч пользователей. Возвращает суммарное количество реально затронутых строк БД.
     * Схема: groups -> accounts (UPDATE→INSERT) -> link table -> post-processing.
     */
    @Transactional
    public long upsertBatch(List<AccountDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return 0L;

        var span = perf.start("AccountService.upsertBatch",
                Map.of("users", String.valueOf(dtos.size()),
                        "servers_unique", String.valueOf(
                                dtos.stream().map(AccountDto::getServerId).filter(Objects::nonNull).distinct().count())));

        long affected = 0;

        // 1) groups (insert-only)
        affected += insertGroups(dtos);
        span.lap("insertGroups");

        // 2) accounts: UPDATE → INSERT (anti-join). НИКАКОГО MERGE/ON CONFLICT.
        affected += upsertAccountsTwoStep(dtos);
        span.lap("upsertAccountsTwoStep");

        // 3) account-group relations (insert-only без дублей)
        affected += insertAccountGroupRelations(dtos);
        span.lap("insertAccountGroupRelations");

        // 4) post-processing per server
        List<Long> serverIds = dtos.stream()
                .map(AccountDto::getServerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!serverIds.isEmpty()) {
            affected += markDeadForServers(serverIds);
            span.lap("markDeadForServers");

            affected += deleteDeadLinksByServers(serverIds);
            span.lap("deleteDeadLinksByServers");
        } else {
            span.annotate("no_servers", Map.of("value", "true"));
        }

        span.finish("ok", Map.of("affected_total", String.valueOf(affected)));
        return affected;
    }

    /** Массовая загрузка групп (insert-only). Возвращает количество вставленных строк. */
    @Transactional
    public long upsertGroups(Set<GroupDto> groupDtos) {
        return insertGroups(groupDtos);
    }

    /* ======================= GROUPS: insert-only ======================= */

    /** insert-only по группам на основании списка аккаунтов. Возвращает rows_written. */
    @Transactional
    long insertGroups(List<AccountDto> dtos) {
        long t0 = System.nanoTime();

        Set<GroupRecord> unique = dtos.stream()
                .filter(d -> d.getGroups() != null)
                .flatMap(d -> d.getGroups().stream().map(g -> new GroupRecord(trimOrNull(g), d.getServerId())))
                .filter(gr -> gr.name() != null && !gr.name().isBlank() && gr.serverId() != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) return 0L;

        String row = "(?, ?, CAST(? AS timestamp))";
        String values = repeat(row, unique.size());

        // insert-only через NOT EXISTS. Advisory lock можно не использовать (уникальный индекс ("group", server_id)).
        String sql = """
            WITH src("group", server_id, updated_at) AS ( VALUES %s )
            INSERT INTO groups ("group", server_id, updated_at)
            SELECT s."group", s.server_id, s.updated_at
            FROM src s
            WHERE NOT EXISTS (
                SELECT 1 FROM groups g
                WHERE g."group" = s."group" AND g.server_id = s.server_id
            );
        """.formatted(values);

        LocalDateTime now = LocalDateTime.now();
        List<Object> params = new ArrayList<>(unique.size() * 3);
        for (GroupRecord gr : unique) {
            params.add(gr.name());
            params.add(gr.serverId());
            params.add(now);
        }
        int written = jdbcTemplate.update(sql, params.toArray());

        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.insertGroups", Map.of("rows_in", String.valueOf(unique.size())))
                .annotate("stats", Map.of("rows_written", String.valueOf(written)))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return written;
    }

    /** insert-only по множеству групп. Возвращает rows_written. */
    @Transactional
    long insertGroups(Set<GroupDto> groupDtos) {
        if (groupDtos == null || groupDtos.isEmpty()) return 0L;

        long t0 = System.nanoTime();

        Set<GroupRecord> unique = groupDtos.stream()
                .filter(Objects::nonNull)
                .map(g -> new GroupRecord(trimOrNull(g.getGroup()), g.getServerId()))
                .filter(gr -> gr.name() != null && !gr.name().isBlank() && gr.serverId() != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) return 0L;

        String row = "(?, ?, CAST(? AS timestamp))";
        String values = repeat(row, unique.size());

        String sql = """
            WITH src("group", server_id, updated_at) AS ( VALUES %s )
            INSERT INTO groups ("group", server_id, updated_at)
            SELECT s."group", s.server_id, s.updated_at
            FROM src s
            WHERE NOT EXISTS (
                SELECT 1 FROM groups g
                WHERE g."group" = s."group" AND g.server_id = s.server_id
            );
        """.formatted(values);

        LocalDateTime now = LocalDateTime.now();
        List<Object> params = new ArrayList<>(unique.size() * 3);
        for (GroupRecord gr : unique) {
            params.add(gr.name());
            params.add(gr.serverId());
            params.add(now);
        }
        int written = jdbcTemplate.update(sql, params.toArray());

        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.insertGroups.set", Map.of("rows_in", String.valueOf(groupDtos.size())))
                .annotate("stats", Map.of("rows_written", String.valueOf(written)))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return written;
    }

    /* ======================= ACCOUNTS: UPDATE → INSERT ======================= */

    /** Внешний метод-обёртка для совместимости. Возвращает rows_affected. */
    @Transactional
    public long insertAccounts(List<AccountDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return 0L;
        return upsertAccountsTwoStep(dtos);
    }

    /**
     * «Двухшаговый» апсерт:
     *  1) UPDATE accounts … FROM src WHERE key AND CHANGES
     *  2) INSERT … SELECT FROM src LEFT JOIN accounts WHERE a IS NULL
     *
     * Нет MERGE и нет ON CONFLICT — не создаём спекулятивных вставок в уникальный индекс.
     */
    @Transactional
    long upsertAccountsTwoStep(List<AccountDto> dtos) {
        long t0 = System.nanoTime();

        // дедуп по (login, server_id)
        LinkedHashMap<Pair<String, Long>, AccountDto> uniq = new LinkedHashMap<>();
        for (AccountDto d : dtos) {
            if (d.getLogin() != null && d.getServerId() != null) {
                uniq.put(new Pair<>(d.getLogin(), d.getServerId()), d);
            }
        }
        List<AccountDto> src = new ArrayList<>(uniq.values());
        if (src.isEmpty()) return 0L;

        String row = "(?, ?, CAST(? AS integer), CAST(? AS text), CAST(? AS text), CAST(? AS text), CAST(? AS text), CAST(? AS timestamp), CAST(? AS boolean))";
        String values = repeat(row, src.size());

        String CHANGES = """
               a.uid             IS DISTINCT FROM s.uid
            OR a.home_dir        IS DISTINCT FROM s.home_dir
            OR a.shell           IS DISTINCT FROM s.shell
            OR a.description     IS DISTINCT FROM s.description
            OR a.account_expires IS DISTINCT FROM s.account_expires
            OR a.active          IS DISTINCT FROM s.active
        """;

        // 1) UPDATE только когда есть реальные изменения
        String sqlUpdate = ("""
            WITH src(login, server_id, uid, home_dir, shell, description, data, account_expires, active) AS ( VALUES %s )
            UPDATE accounts a
               SET uid             = COALESCE(s.uid, a.uid),
                   home_dir        = COALESCE(s.home_dir, a.home_dir),
                   shell           = COALESCE(s.shell, a.shell),
                   description     = COALESCE(s.description, a.description),
                   account_expires = COALESCE(s.account_expires, a.account_expires),
                   active          = COALESCE(s.active, a.active),
                   last_seen       = now(),
                   updated_at      = now()
              FROM src s
             WHERE a.login = s.login
               AND a.server_id = s.server_id
               AND (%s)
        """).formatted(values, CHANGES);

        // 2) INSERT только тех, кого нет
        String sqlInsert = ("""
            WITH src(login, server_id, uid, home_dir, shell, description, data, account_expires, active) AS ( VALUES %s )
            INSERT INTO accounts
                (login, server_id, uid, home_dir, shell, description, data, account_expires, last_seen, active, updated_at)
            SELECT s.login, s.server_id, s.uid, s.home_dir, s.shell, s.description, s.data, s.account_expires, now(), s.active, now()
              FROM src s
              LEFT JOIN accounts a
                ON a.login = s.login AND a.server_id = s.server_id
             WHERE a.login IS NULL
        """).formatted(values);

        List<Object> params = new ArrayList<>(src.size() * 9);
        for (AccountDto d : src) {
            params.add(d.getLogin());
            params.add(d.getServerId());
            params.add(d.getUid());
            params.add(trimOrNull(d.getHomeDir()));
            params.add(trimOrNull(d.getShell()));
            params.add(trimOrNull(d.getDescription()));
            params.add(d.getData());
            params.add(d.getAccountExpires());
            params.add(d.getActive());
        }

        int upd = jdbcTemplate.update(sqlUpdate, params.toArray());
        int ins = jdbcTemplate.update(sqlInsert, params.toArray());
        int affected = upd + ins;

        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.upsertAccountsTwoStep", Map.of("rows_in", String.valueOf(src.size())))
                .annotate("stats", Map.of("updated", String.valueOf(upd), "inserted", String.valueOf(ins)))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return affected;
    }

    /* ========== accounts_in_groups: insert-only без дубликатов ========== */

    /** Возвращает количество созданных связей. */
    @Transactional
    long insertAccountGroupRelations(List<AccountDto> dtos) {
        long t0 = System.nanoTime();

        Map<Pair<String, Long>, Long> accountIdMap = fetchAccountIds(dtos);
        Map<Pair<String, Long>, Long> groupIdMap   = fetchGroupIds(dtos);

        List<Pair<Long, Long>> pairs = new ArrayList<>();
        for (AccountDto dto : dtos) {
            Long accId = accountIdMap.get(new Pair<>(dto.getLogin(), dto.getServerId()));
            if (accId == null || dto.getGroups() == null) continue;
            for (String g : dto.getGroups()) {
                Long grId = groupIdMap.get(new Pair<>(g, dto.getServerId()));
                if (grId != null) pairs.add(new Pair<>(accId, grId));
            }
        }
        if (pairs.isEmpty()) {
            perf.start("AccountService.insertAccountGroupRelations", Map.of("rows_in", "0"))
                    .annotate("stats", Map.of("rows_written", "0"))
                    .finish("ok", Map.of("ms", "0"));
            return 0L;
        }

        List<Pair<Long, Long>> uniqPairs = new ArrayList<>(new LinkedHashSet<>(pairs));

        String row = "(CAST(? AS bigint), CAST(? AS bigint), CAST(? AS timestamp))";
        String values = repeat(row, uniqPairs.size());

        String sql = """
            WITH src(account_id, group_id, updated_at) AS ( VALUES %s )
            INSERT INTO accounts_in_groups (account_id, group_id, updated_at)
            SELECT s.account_id, s.group_id, s.updated_at
            FROM src s
            LEFT JOIN accounts_in_groups t
              ON t.account_id = s.account_id AND t.group_id = s.group_id
            WHERE t.account_id IS NULL
        """.formatted(values);

        LocalDateTime now = LocalDateTime.now();
        List<Object> params = new ArrayList<>(uniqPairs.size() * 3);
        for (Pair<Long, Long> p : uniqPairs) {
            params.add(p.first());
            params.add(p.second());
            params.add(now);
        }
        int written = jdbcTemplate.update(sql, params.toArray());

        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.insertAccountGroupRelations", Map.of("rows_in", String.valueOf(uniqPairs.size())))
                .annotate("stats", Map.of("rows_written", String.valueOf(written)))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return written;
    }

    /* ======================= DEAD-метки и чистка связей ======================= */

    private static final String MARK_DEAD_SQL = """
        WITH ref AS (
            SELECT server_id, MAX(last_seen) AS max_seen
            FROM accounts
            WHERE server_id = ANY (?::bigint[])
            GROUP BY server_id
        )
        UPDATE accounts a
           SET dead = true,
               updated_at = now()
          FROM ref r
         WHERE a.server_id = r.server_id
           AND a.last_seen < r.max_seen - interval '10 seconds'
           AND a.dead IS DISTINCT FROM true
    """;

    /** Помечает "dead" старые записи. Возвращает rows_updated. */
    long markDeadForServers(List<Long> serverIds) {
        long t0 = System.nanoTime();
        if (serverIds == null || serverIds.isEmpty()) return 0L;
        List<Long> distinct = serverIds.stream().filter(Objects::nonNull).distinct().toList();
        int updated = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(MARK_DEAD_SQL);
            ps.setArray(1, con.createArrayOf("bigint", distinct.toArray(Long[]::new)));
            return ps;
        });
        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.markDeadForServers", Map.of("servers", String.valueOf(distinct.size())))
                .annotate("stats", Map.of("rows_updated", String.valueOf(updated)))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return updated;
    }

    private static final String DELETE_DEAD_LINKS_SQL = """
        DELETE FROM accounts_in_groups aig
        USING accounts a
        WHERE a.server_id = ANY (?::bigint[])
          AND a.dead = true
          AND aig.account_id = a.id
    """;

    /** Удаляет связи мёртвых аккаунтов. Возвращает rows_deleted. */
    long deleteDeadLinksByServers(List<Long> serverIds) {
        long t0 = System.nanoTime();
        if (serverIds == null || serverIds.isEmpty()) return 0L;
        List<Long> distinct = serverIds.stream().filter(Objects::nonNull).distinct().toList();
        int deleted = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(DELETE_DEAD_LINKS_SQL);
            ps.setArray(1, con.createArrayOf("bigint", distinct.toArray(Long[]::new)));
            return ps;
        });
        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.deleteDeadLinksByServers", Map.of("servers", String.valueOf(distinct.size())))
                .annotate("stats", Map.of("rows_deleted", String.valueOf(deleted)))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return deleted;
    }

    /* ======================= FETCH HELPERS ======================= */

    @Transactional(Transactional.TxType.SUPPORTS)
    Map<Pair<String, Long>, Long> fetchAccountIds(List<AccountDto> dtos) {
        long t0 = System.nanoTime();
        Set<Pair<String, Long>> keys = dtos.stream()
                .filter(d -> d.getLogin() != null && d.getServerId() != null)
                .map(d -> new Pair<>(d.getLogin(), d.getServerId()))
                .collect(Collectors.toSet());
        if (keys.isEmpty()) return Map.of();

        String row = "(?, ?)";
        String values = repeat(row, keys.size());
        String sql = """
            SELECT a.id, a.login, a.server_id
            FROM accounts a
            JOIN (VALUES %s) AS v(login, server_id)
              ON a.login = v.login AND a.server_id = v.server_id
        """.formatted(values);

        List<Object> params = new ArrayList<>(keys.size() * 2);
        keys.forEach(k -> { params.add(k.first()); params.add(k.second()); });

        Map<Pair<String, Long>, Long> result = jdbcTemplate.query(sql, params.toArray(), rs -> {
            Map<Pair<String, Long>, Long> map = new HashMap<>();
            while (rs.next()) {
                map.put(new Pair<>(rs.getString(2), rs.getLong(3)), rs.getLong(1));
            }
            return map;
        });

        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.fetchAccountIds", Map.of("keys", String.valueOf(keys.size())))
                .annotate("stats", Map.of("found", String.valueOf(result.size())))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return result;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    Map<Pair<String, Long>, Long> fetchGroupIds(List<AccountDto> dtos) {
        long t0 = System.nanoTime();
        Set<Pair<String, Long>> keys = dtos.stream()
                .filter(d -> d.getGroups() != null && d.getServerId() != null)
                .flatMap(d -> d.getGroups().stream().map(g -> new Pair<>(g, d.getServerId())))
                .map(p -> new Pair<>(trimOrNull(p.first()), p.second()))
                .collect(Collectors.toSet());
        if (keys.isEmpty()) return Map.of();

        String row = "(?, ?)";
        String values = repeat(row, keys.size());
        String sql = """
            SELECT g.id, g."group", g.server_id
            FROM groups g
            JOIN (VALUES %s) AS v("group", server_id)
              ON g."group" = v."group" AND g.server_id = v.server_id
        """.formatted(values);

        List<Object> params = new ArrayList<>(keys.size() * 2);
        keys.forEach(k -> { params.add(k.first()); params.add(k.second()); });

        Map<Pair<String, Long>, Long> result = jdbcTemplate.query(sql, params.toArray(), rs -> {
            Map<Pair<String, Long>, Long> map = new HashMap<>();
            while (rs.next()) {
                map.put(new Pair<>(rs.getString(2), rs.getLong(3)), rs.getLong(1));
            }
            return map;
        });

        long ms = (System.nanoTime() - t0) / 1_000_000;
        perf.start("AccountService.fetchGroupIds", Map.of("keys", String.valueOf(keys.size())))
                .annotate("stats", Map.of("found", String.valueOf(result.size())))
                .finish("ok", Map.of("ms", String.valueOf(ms)));
        return result;
    }

    /* ======================= CRUD-методы (точечные) ======================= */

    public boolean existsAccount(String login, Long serverId) {
        String sql = "SELECT 1 FROM accounts WHERE login = ? AND server_id = ? LIMIT 1";
        List<Integer> r = jdbcTemplate.query(sql, (rs, rn) -> 1, login, serverId);
        return !r.isEmpty();
    }

    public void addUserToGroup(Long accountId, Long groupId) {
        String sql = """
            INSERT INTO accounts_in_groups (account_id, group_id, updated_at)
            SELECT CAST(? AS bigint), CAST(? AS bigint), now()
            WHERE NOT EXISTS (
                SELECT 1 FROM accounts_in_groups t
                WHERE t.account_id = CAST(? AS bigint) AND t.group_id = CAST(? AS bigint)
            )
        """;
        jdbcTemplate.update(sql, accountId, groupId, accountId, groupId);
    }

    public void removeUserFromGroup(Long accountId, Long groupId) {
        jdbcTemplate.update("DELETE FROM accounts_in_groups WHERE account_id = ? AND group_id = ?", accountId, groupId);
    }

    public void deleteById(Long accountId) {
        jdbcTemplate.update("DELETE FROM accounts_in_groups WHERE account_id = ?", accountId);
        jdbcTemplate.update("UPDATE accounts SET dead = true, updated_at = now() WHERE id = ?", accountId);
    }

    public void updateById(AccountDto dto, Long accountId) {
        StringBuilder sql = new StringBuilder("UPDATE accounts SET ");
        List<Object> params = new ArrayList<>();

        if (dto.getLogin() != null)          { sql.append("login = ?, ");             params.add(dto.getLogin()); }
        if (dto.getServerId() != null)       { sql.append("server_id = ?, ");         params.add(dto.getServerId()); }
        if (dto.getUid() != null)            { sql.append("uid = ?, ");               params.add(dto.getUid()); }
        if (dto.getHomeDir() != null)        { sql.append("home_dir = ?, ");          params.add(dto.getHomeDir()); }
        if (dto.getShell() != null)          { sql.append("shell = ?, ");             params.add(dto.getShell()); }
        if (dto.getDescription() != null)    { sql.append("description = ?, ");       params.add(dto.getDescription()); }
        if (dto.getData() != null)           { sql.append("data = ?, ");              params.add(dto.getData()); }
        if (dto.getAccountExpires() != null) { sql.append("account_expires = ?, ");   params.add(dto.getAccountExpires()); }
        if (dto.getLastSeen() != null)       { sql.append("last_seen = ?, ");         params.add(dto.getLastSeen()); }
        if (dto.getActive() != null)         { sql.append("active = ?, ");            params.add(dto.getActive()); }

        sql.append("updated_at = now() WHERE id = ?");
        params.add(accountId);

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /* ======================= Утилиты ======================= */

    private static String repeat(String rowPattern, int n) {
        StringJoiner j = new StringJoiner(", ");
        for (int i = 0; i < n; i++) j.add(rowPattern);
        return j.toString();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private record GroupRecord(String name, Long serverId) { }
    private record Pair<T, U>(T first, U second) {}
}
