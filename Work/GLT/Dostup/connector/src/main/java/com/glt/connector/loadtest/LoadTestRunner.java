package com.glt.connector.loadtest;

import com.glt.connector.dto.Account.AccountBatch;
import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.model.Server;
import com.glt.connector.model.enums.OperatingSystemType;
import com.glt.connector.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Полный путь для лоад-теста:
 *  1) При необходимости — seed N серверов в БД.
 *  2) Генерим accountsTotal пользователей (равномерно по серверам).
 *  3) Отправляем БАТЧИ AccountBatch в Kafka (users-topic).
 *  4) Дальше ваш AccountConsumer читает и пишет в БД.
 *
 * Настройки — через loadtest.* в application.properties или CLI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoadTestRunner implements CommandLineRunner, InitializingBean {

    private final ServerRepository serverRepository;

    @Autowired(required = false)
    private KafkaTemplate<String, AccountBatch> kafkaTemplate;

    // ===== Конфиг =====
    @Value("${loadtest.enabled:false}")        private boolean enabled;
    @Value("${loadtest.start-delay-ms:2000}")  private long startDelayMs;

    @Value("${loadtest.servers:10000}")        private int serversTarget;
    @Value("${loadtest.accounts:1000000}")     private long accountsTotal;
    @Value("${loadtest.batch-size:1000}")      private int batchSize;
    @Value("${loadtest.concurrency:4}")        private int concurrency;
    @Value("${loadtest.seed-servers:true}")    private boolean seedServers;

    @Value("${connector.kafka.users-topic:users-topic}")
    private String usersTopic;

    // bounded-пул и back-pressure на продюсере
    private ThreadPoolExecutor executor;
    private final Semaphore kafkaInflight = new Semaphore(500); // потолок одновременных send'ов

    @Override
    public void afterPropertiesSet() {
        if (!enabled) return;
        executor = new ThreadPoolExecutor(
                concurrency, concurrency,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(concurrency * 2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        log.info("[LOADTEST] executor created: threads={}, queue={}", concurrency, concurrency * 2);
    }

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) {
            log.info("[LOADTEST] disabled (loadtest.enabled=false)");
            return;
        }
        if (kafkaTemplate == null) {
            throw new IllegalStateException("KafkaTemplate<String, AccountBatch> не найден. Проверь конфиг продюсера/бинов.");
        }

        // подождём, чтобы ваши консюмеры поднялись
        if (startDelayMs > 0) {
            log.info("[LOADTEST] waiting {} ms for services to start...", startDelayMs);
            Thread.sleep(startDelayMs);
        }

        log.info("[LOADTEST] START: servers={}, accounts={}, batchSize={}, threads={}, usersTopic={}",
                serversTarget, accountsTotal, batchSize, concurrency, usersTopic);

        long t0 = System.nanoTime();

        List<Long> serverIds = seedServers ? ensureServers(serversTarget) : fetchServers(serversTarget);
        if (serverIds.size() < serversTarget) {
            log.warn("[LOADTEST] only {} servers present (requested {})", serverIds.size(), serversTarget);
        }

        // равномерно распределяем пользователей по серверам
        final int S = serverIds.size();
        if (S == 0) {
            log.error("[LOADTEST] no servers available");
            return;
        }
        long basePerServer = accountsTotal / S;
        int remainder = (int) (accountsTotal % S);

        AtomicLong produced = new AtomicLong(0);
        List<Future<?>> futures = new ArrayList<>(S);

        for (int i = 0; i < S; i++) {
            final long srvId = serverIds.get(i);
            final long countForServer = basePerServer + (i < remainder ? 1 : 0);
            futures.add(executor.submit(() -> generateForServer(srvId, countForServer, produced)));
        }

        // дождаться завершения всех задач
        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException e) { log.error("[LOADTEST] worker failed", e.getCause()); }
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        long tookMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("[LOADTEST] DONE: produced={} accounts in {} ms", produced.get(), tookMs);
    }

    private void generateForServer(long serverId, long countForServer, AtomicLong produced) {
        final String[] groupPool = {"dev", "ops", "hr", "sales", "net", "db", "sec", "qa"};
        List<AccountDto> buffer = new ArrayList<>(batchSize);

        long sent = 0;
        long uidBase = serverId * 100_000L;

        while (sent < countForServer) {
            buffer.clear();
            int n = (int) Math.min(batchSize, countForServer - sent);

            for (int i = 0; i < n; i++) {
                long idx = sent + i;
                AccountDto dto = new AccountDto();
                dto.setLogin("user_" + serverId + "_" + idx);
                dto.setServerId(serverId);
                dto.setUid((int) ((uidBase + idx) % Integer.MAX_VALUE));
                dto.setHomeDir("/home/" + dto.getLogin());
                dto.setShell("/bin/bash");
                dto.setDescription("LT user " + idx);
                dto.setData("{\"source\":\"loadtest\"}");
                dto.setAccountExpires(null);
                dto.setActive(Boolean.TRUE);
                dto.setGroups(Set.of(
                        groupPool[(int)((idx + 0) % groupPool.length)],
                        groupPool[(int)((idx + 3) % groupPool.length)]
                ));
                buffer.add(dto);
            }

            // отправляем пакет в Kafka → дальше ваш AccountConsumer пишет в БД
            sendKafkaBatch(buffer);

            sent += n;
            long total = produced.addAndGet(n);
            if ((total % 100_000) == 0) {
                log.info("[LOADTEST] produced: {} accounts", total);
            }
        }
    }

    private void sendKafkaBatch(List<AccountDto> dtos) {
        String key = "lt-" + UUID.randomUUID();
        kafkaInflight.acquireUninterruptibly();
        kafkaTemplate.send(usersTopic, key, new AccountBatch(new ArrayList<>(dtos)))
                .whenComplete((res, ex) -> {
                    kafkaInflight.release();
                    if (ex != null) {
                        log.error("[LOADTEST] send failed for key={}, err={}", key, ex.toString(), ex);
                    }
                });
    }

    @Transactional(readOnly = true)
    protected List<Long> fetchServers(int limit) {
        // страхуемся от нулевого лимита
        if (limit <= 0) return List.of();

        // стабильная пагинация по id ASC (чтоб не было пропусков/дубликатов)
        final int pageSize = Math.min(1000, limit); // можно настроить
        final Sort sort = Sort.by(Sort.Direction.ASC, "id");

        List<Long> ids = new ArrayList<>(limit);
        int page = 0;

        while (ids.size() < limit) {
            Page<Server> result = serverRepository.findByActiveTrue(PageRequest.of(page, pageSize, sort));
            if (result.isEmpty()) break;

            for (Server s : result.getContent()) {
                ids.add(s.getId());
                if (ids.size() == limit) break;
            }

            if (!result.hasNext()) break;
            page++;
        }
        return ids;
    }

    @Transactional
    protected List<Long> ensureServers(int target) {
        List<Long> existing = fetchServers(target);
        int need = target - existing.size();
        if (need <= 0) return existing;

        List<Server> toSave = new ArrayList<>(need);
        int base = existing.size();
        for (int i = 0; i < need; i++) {
            int idx = base + i + 1;
            Server s = new Server();
            s.setHost("10.200." + (idx / 256) + "." + (idx % 256));
            s.setPort(22);
            s.setLogin("root");
            s.setPassword("dummy");
            s.setOsType(OperatingSystemType.UBUNTU);
            s.setActive(true);
            s.setLastScannedAt(null);
            s.setScanIntervalMinutes(0);
            toSave.add(s);
        }
        serverRepository.saveAll(toSave);

        List<Long> all = fetchServers(target);
        log.info("[LOADTEST] seeded {} servers (total now {})", need, all.size());
        return all;
    }
}
