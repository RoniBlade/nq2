package com.glt.connector.service.kafka;

import com.glt.connector.dto.Account.AccountBatch;
import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.model.TaskLog;
import com.glt.connector.repository.ServerRepository;
import com.glt.connector.repository.TaskLogRepository;
import com.glt.connector.service.PerfLogger;
import com.glt.connector.service.ServerScannerRegistry;
import com.glt.connector.task.ScanTask;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanTaskWorker {

    private final ServerRepository serverRepository;
    private final ServerScannerRegistry scannerRegistry;
    private final KafkaTemplate<String, AccountBatch> kafkaTemplate;
    private final RetryTemplate retryTemplate;
    private final TaskLogRepository taskLogRepository;
    private final ObjectProvider<KafkaConsumer<String, ScanTask>> consumerProvider;
    private final PerfLogger perf;

    @Value("${connector.kafka.scan-tasks-topic}")
    private String scanTasksTopic;

    @Value("${connector.kafka.users-topic}")
    private String usersTopic;

    @Value("${connector.scan.thread-pool-size}")
    private int threadPoolSize;

    /** лимит одновременно идущих коннектов к одному IP */
    @Value("${connector.scan.per-host-limit:4}")
    private int perHostLimit;

    /** bounded executor */
    private ThreadPoolExecutor executor;

    /** back-pressure на отправку users */
    private final Semaphore inflightUsers = new Semaphore(200);

    /** текущий consumer, чтобы wakeup в shutdown */
    private final AtomicReference<KafkaConsumer<String, ScanTask>> currentConsumer = new AtomicReference<>();

    /** пер-IP семафоры */
    private final ConcurrentHashMap<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();

    /** max завершённые оффсеты по партициям */
    private final ConcurrentMap<TopicPartition, Long> completedOffsets = new ConcurrentHashMap<>();

    /** локальный бэклог задач, которые сейчас не поместились по IP-лимиту */
    private final Deque<Pending> backlog = new ConcurrentLinkedDeque<>();

    private Thread consumerThread;
    private String hostname;

    public void start() {
        executor = new ThreadPoolExecutor(
                threadPoolSize, threadPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threadPoolSize * 2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        hostname = resolveHostname();
        log.info("[INIT] ScanTaskWorker on '{}' pool={}", hostname, threadPoolSize);

        consumerThread = new Thread(this::consumeLoop, "ScanTask-Kafka-Consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("[INIT] Kafka consumer thread started");
    }

    @PreDestroy
    public void shutdown() {
        log.info("[SHUTDOWN] ScanTaskWorker stopping...");
        var c = currentConsumer.get();
        if (c != null) {
            try { c.wakeup(); } catch (Exception ignored) {}
        }
        if (executor != null) executor.shutdown();
        log.info("[SHUTDOWN] executor shutdown initiated");
    }

    private void consumeLoop() {
        try (KafkaConsumer<String, ScanTask> consumer = consumerProvider.getObject()) {
            currentConsumer.set(consumer);
            consumer.subscribe(Collections.singletonList(scanTasksTopic));
            log.info("[SUBSCRIBE] {}", scanTasksTopic);

            // будем получать результаты задач именно как Completed (tp, offset)
            CompletionService<Completed> completion = new ExecutorCompletionService<>(executor);

            while (true) {
                try {
                    maybePauseOrResume(consumer);

                    ConsumerRecords<String, ScanTask> records = consumer.poll(Duration.ofMillis(500));
                    int submitted = 0;

                    if (!records.isEmpty()) {
                        // 1) Первая волна сабмитов — только для тех, у кого tryAcquire прошёл
                        for (ConsumerRecord<String, ScanTask> r : records) {
                            final TopicPartition tp = new TopicPartition(r.topic(), r.partition());
                            final long offset = r.offset();
                            final ScanTask task = r.value();
                            final String ip = task.getServerIp();
                            final Long serverId = task.getServerId();

                            final Semaphore sem = hostSemaphores.computeIfAbsent(ip, k -> new Semaphore(perHostLimit));
                            if (sem.tryAcquire()) {
                                completion.submit(() -> submitWithHeldPermit(task, sem, tp, offset));
                                // заносим минимальный оффсет для партиции, если его ещё нет
                                completedOffsets.putIfAbsent(tp, offset - 1);
                                submitted++;
                                log.debug("[ENQUEUE] serverId={}, ip={}, tp={}, offset={}", serverId, ip, tp, offset);
                            } else {
                                // нет слота — в бэклог
                                backlog.addLast(new Pending(task, sem, tp, offset));
                                log.debug("[BACKLOG] per-host limit: serverId={}, ip={}, backlog={}", serverId, ip, backlog.size());
                            }
                        }

                        // 2) Попытки выдать из бэклога (несколько проходов достаточно)
                        int spins = 0;
                        while (!backlog.isEmpty() && spins++ < 3) {
                            int n = backlog.size();
                            for (int i = 0; i < n; i++) {
                                Pending p = backlog.pollFirst();
                                if (p == null) break;
                                if (p.sem.tryAcquire()) {
                                    completion.submit(() -> submitWithHeldPermit(p.task, p.sem, p.tp, p.offset));
                                    completedOffsets.putIfAbsent(p.tp, p.offset - 1);
                                    submitted++;
                                    log.debug("[BACKLOG->RUN] serverId={}, ip={}, tp={}, offset={}",
                                            p.task.getServerId(), p.task.getServerIp(), p.tp, p.offset);
                                } else {
                                    backlog.addLast(p); // пока не можем — вернём назад
                                }
                            }
                        }
                    }

                    // 3) Собираем готовые результаты и обновляем max завершённые оффсеты
                    int toCollect = submitted;
                    for (int i = 0; i < toCollect; i++) {
                        Future<Completed> f = completion.poll();
                        if (f == null) break;
                        try {
                            Completed c = f.get();
                            // по конкретной партиции обновляем max завершённый оффсет
                            completedOffsets.merge(c.tp, c.offset, Math::max);
                        } catch (ExecutionException ee) {
                            log.error("[TASK-ERR]", ee.getCause());
                        }
                    }

                    // 4) Коммитим снапшот max завершённых оффсетов
                    if (!completedOffsets.isEmpty()) {
                        Map<TopicPartition, OffsetAndMetadata> snapshot = new HashMap<>();
                        for (Map.Entry<TopicPartition, Long> e : completedOffsets.entrySet()) {
                            long next = e.getValue() + 1;
                            if (next > 0) snapshot.put(e.getKey(), new OffsetAndMetadata(next));
                        }
                        if (!snapshot.isEmpty()) {
                            consumer.commitAsync(snapshot, (offsets, ex) -> {
                                if (ex != null) log.warn("[COMMIT-ASYNC] err={}", ex.toString(), ex);
                            });
                        }
                    }
                } catch (WakeupException e) {
                    log.info("[WAKEUP] Stop signal");
                    break;
                }
            }
        } catch (Exception e) {
            log.error("[ERROR] consumeLoop", e);
        } finally {
            currentConsumer.set(null);
            log.info("[CLOSE] Kafka consumer closed");
        }
    }

    private void maybePauseOrResume(KafkaConsumer<String, ScanTask> consumer) {
        int q = executor.getQueue().size();
        int usedInflight = 200 - inflightUsers.availablePermits();
        // учитываем и очередь пула, и забитость отправки users
        if (q > threadPoolSize * 2 || usedInflight > 180) {
            consumer.pause(consumer.assignment());
        } else if (q < threadPoolSize && usedInflight < 100) {
            consumer.resume(consumer.assignment());
        }
    }

    /** Сабмит задачи с уже захваченным семафором; освобождаем слот в finally */
    private Completed submitWithHeldPermit(ScanTask task, Semaphore sem, TopicPartition tp, long offset) {
        try {
            processScanTaskCore(task);
            return new Completed(tp, offset);
        } finally {
            sem.release();
        }
    }

    /** Новый core-вариант без проверки isDue — задача уже отобрана и зарезервирована Creator'ом. */
    private void processScanTaskCore(ScanTask task) {
        String taskId = task.getTaskId();
        String ip = task.getServerIp();
        Long serverId = task.getServerId();

        var span = perf.start("ScanTaskWorker", Map.of(
                "taskId", taskId,
                "ip", ip,
                "host", hostname,
                "serverId", String.valueOf(serverId),
                "os", String.valueOf(task.getOsType())
        ));

        boolean attemptedMarked = false;

        try {
            // 1) Отмечаем попытку — как только реально начинаем
            try {
                serverRepository.markScanAttempted(serverId);
                attemptedMarked = true;
            } catch (Exception markEx) {
                log.warn("[Worker] markScanAttempted failed: serverId={}, ip={}, err={}",
                        serverId, ip, markEx.toString());
            }

            // 2) Скан с ретраями
            span.lap("scanUsers_start");
            List<AccountDto> users = retryTemplate.execute(ctx -> {
                var res = scannerRegistry.getScanner(task.getOsType()).scanUsers(task);
                log.debug("[RETRY] attempt #{} for taskId={}, serverId={}, ip={}",
                        ctx.getRetryCount() + 1, taskId, serverId, ip);
                return res;
            });
            span.lap("scanUsers_done");
            span.annotate("counts", Map.of("users", String.valueOf(users.size())));

            // 3) Отправка результата (бэкап-ограничение по семафору)
            span.lap("sendKafka_try");
            if (!inflightUsers.tryAcquire(250, TimeUnit.MILLISECONDS)) {
                log.warn("[Worker] inflightUsers saturated; skipping immediate send taskId={}, serverId={}, ip={}",
                        taskId, serverId, ip);
            } else {
                kafkaTemplate.send(usersTopic, taskId, new AccountBatch(users))
                        .whenComplete((r, ex) -> {
                            inflightUsers.release();
                            if (ex != null) {
                                span.annotate("sendKafka", Map.of("status", "fail", "err", ex.toString()));
                                log.error("[SEND-FAIL users] taskId={}, serverId={}, ip={}, err={}",
                                        taskId, serverId, ip, ex.toString(), ex);
                            } else {
                                span.lap("sendKafka_ack");
                            }
                        });
            }

            log.info("[PROCESS] Done: taskId={}, serverId={}, ip={}, users={}, host={}",
                    taskId, serverId, ip, users.size(), hostname);

            taskLogRepository.save(TaskLog.builder()
                    .serverIp(ip)
                    .taskType("SCAN")
                    .status("SUCCESS")
                    .message("serverId=" + serverId + " Пользователей: " + users.size())
                    .timestamp(LocalDateTime.now())
                    .build());

            span.finish("ok", Map.of());

        } catch (Exception e) {
            span.annotate("exception", Map.of("message", e.getMessage() == null ? "unknown" : e.getMessage()));
            span.finish("error", Map.of());
            log.error("[ERROR] Scan failed: taskId={}, serverId={}, ip={}, host={}, err={}",
                    taskId, serverId, ip, hostname, e.getMessage(), e);

            taskLogRepository.save(TaskLog.builder()
                    .serverIp(ip)
                    .taskType("SCAN")
                    .status("FAILED")
                    .message("serverId=" + serverId + " Ошибка: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        } finally {
            // 4) Гарантированно отмечаем завершение и снимаем резерв
            if (!attemptedMarked) {
                try { serverRepository.markScanAttempted(serverId); } catch (Exception ignore) {}
            }
            try { serverRepository.markScanFinished(serverId); } catch (Exception ignore) {}
            safeClearReservation(serverId);
        }
    }

    private void safeClearReservation(Long id) {
        try { serverRepository.clearReservation(id); }
        catch (Exception e) { log.debug("[Worker] clearReservation failed id={} err={}", id, e.toString()); }
    }

    private String resolveHostname() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "unknown-host"; }
    }

    /* ==== утилиты ==== */

    private record Pending(ScanTask task, Semaphore sem, TopicPartition tp, long offset) {}
    private record Completed(TopicPartition tp, long offset) {}
}
