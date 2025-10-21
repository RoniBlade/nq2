package com.glt.connector.service.kafka;

import com.glt.connector.model.Server;
import com.glt.connector.model.enums.OperatingSystemType;
import com.glt.connector.repository.ServerRepository;
import com.glt.connector.service.PerfLogger;
import com.glt.connector.task.ScanTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanTaskCreator {

    private final ServerRepository serverRepository;
    private final KafkaTemplate<String, ScanTask> kafkaTemplate;
    private final PerfLogger perf;

    @Value("${connector.scan.thread-pool-size:3}")
    private Integer threadPoolSize;

    @Value("${connector.scan.batch-size:80}")
    private Integer pageSize;

    @Value("${connector.kafka.scan-tasks-topic}")
    private String topic;

    /** Сколько минут «держится» резервация — такой же параметр, как в репозитории. */
    @Value("${connector.scan.reserve-timeout-minutes:10}")
    private int reserveTimeoutMinutes;

    private final AtomicInteger page = new AtomicInteger(0);

    private ThreadPoolExecutor executor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Semaphore runGuard = new Semaphore(1);

    public void start() {
        if (!started.compareAndSet(false, true)) {
            log.info("[Creator] уже запущен");
            return;
        }
        executor = new ThreadPoolExecutor(
                threadPoolSize, threadPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threadPoolSize * 2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        log.info("[Creator] инициализирован: threads={}, pageSize={}, reserveTimeoutMin={}",
                threadPoolSize, pageSize, reserveTimeoutMinutes);
    }

    /** Одноразовый проход. Вызывается планировщиком. */
    public void scanOnce() {
        if (!started.get()) start();
        if (!runGuard.tryAcquire()) {
            log.warn("[Creator] прошлый проход ещё идёт — пропуск");
            return;
        }

        var pass = perf.start("ScanTaskCreator.scanOnce", Map.of(
                "threads", String.valueOf(threadPoolSize),
                "pageSize", String.valueOf(pageSize),
                "reserveTimeoutMin", String.valueOf(reserveTimeoutMinutes)
        ));

        long t0 = System.nanoTime();
        try {
            // 0) Если есть «свежие» резервации — значит, в Kafka ещё лежит старая очередь → пропускаем проход
            long inflight = serverRepository.countFreshReservations(reserveTimeoutMinutes);
            if (inflight > 0) {
                log.info("[Creator] пропуск обхода: свежих резерваций={} (≤ {} мин). Доедаем старую очередь.",
                        inflight, reserveTimeoutMinutes);
                pass.finish("skip_inflight", Map.of("fresh_reservations", String.valueOf(inflight)));
                return;
            }

            page.set(0);
            List<Future<?>> fs = new ArrayList<>(threadPoolSize);
            for (int i = 0; i < threadPoolSize; i++) {
                final int threadId = i + 1;
                fs.add(executor.submit(() -> pageLoop(threadId)));
            }
            for (Future<?> f : fs) {
                try { f.get(); }
                catch (ExecutionException e) { log.error("[Creator] worker error", e.getCause()); }
            }
            long tookMs = (System.nanoTime() - t0) / 1_000_000;
            log.info("[Creator] проход завершён за {} ms", tookMs);
            pass.finish("ok", Map.of("total_ms", String.valueOf(tookMs)));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("[Creator] прерван");
            pass.finish("interrupted", Map.of());
        } finally {
            runGuard.release();
        }
    }

    private void pageLoop(int threadId) {
        var span = perf.start("ScanTaskCreator.pageLoop", Map.of("thread", String.valueOf(threadId)));
        while (true) {
            int currentPage = page.getAndIncrement();
            span.lap("fetch_page_start");

            Page<Server> result = serverRepository.findEligibleForScan(
                    PageRequest.of(currentPage, pageSize),
                    reserveTimeoutMinutes
            );
            span.lap("fetch_page_done");

            if (result.isEmpty()) {
                log.info("[Creator:T{}] страниц больше нет (page={})", threadId, currentPage);
                span.finish("done", Map.of("last_page", String.valueOf(currentPage - 1)));
                break;
            }

            List<Server> servers = result.getContent();
            if (servers.isEmpty()) continue;

            Long[] ids = servers.stream().map(Server::getId).toArray(Long[]::new);

            long t0 = System.nanoTime();
            int reserved = serverRepository.reserveForScan(ids);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            span.annotate("reserve", Map.of("count", String.valueOf(reserved), "ms", String.valueOf(ms)));
            if (reserved <= 0) continue;

            List<ScanTask> tasks = new ArrayList<>(servers.size());
            for (Server s : servers) {
                tasks.add(toScanTask(s));
            }
            log.info("[Creator:T{}] → Kafka: {} задач (page={})", threadId, tasks.size(), currentPage);

            span.lap("send_start");
            for (ScanTask t : tasks) {
                kafkaTemplate.send(topic, String.valueOf(t.getServerId()), t)
                        .whenComplete((r, ex) -> {
                            if (ex != null) {
                                log.error("[Creator] send fail serverId={} err={}", t.getServerId(), ex.toString(), ex);
                            }
                        });
            }
            span.lap("send_done");
        }
    }

    private ScanTask toScanTask(Server s) {
        return ScanTask.builder()
                .taskId(UUID.randomUUID().toString())
                .serverId(s.getId())
                .serverIp(s.getHost())
                .port(s.getPort() != null ? s.getPort() : 22)
                .login(s.getLogin())
                .encryptedPassword(s.getPassword())
                .osType(s.getOsType() != null ? s.getOsType() : OperatingSystemType.UBUNTU)
                .lastScannedAt(s.getLastScannedAt())
                .scanIntervalMinutes(s.getScanIntervalMinutes())
                .build();
    }
}
