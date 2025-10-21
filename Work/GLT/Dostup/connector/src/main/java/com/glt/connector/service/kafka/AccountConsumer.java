package com.glt.connector.service.kafka;

import com.glt.connector.dto.Account.AccountBatch;
import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.service.AccountService;
import com.glt.connector.service.PerfLogger;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Высокопроизводительный consumer users-topic:
 * - merge -> chunked upsert (разбивка на чанки к БД)
 * - commit offset только после успешной БД-операции всех чанков полла
 * - backpressure через pause/resume по очереди и inflight
 * - PERF-метрики через PerfLogger
 *
 * Заточен под AccountService.upsertBatch(List<AccountDto>) со схемой UPDATE→INSERT (без MERGE/ON CONFLICT).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountConsumer {

    @Qualifier("userAccountKafkaConsumer")
    private final ObjectProvider<KafkaConsumer<String, AccountBatch>> consumerProvider;

    private final AccountService accountService;
    private final PerfLogger perf;

    @Value("${connector.kafka.users-topic}")
    private String usersTopic;

    /** Размер пула задач к БД */
    @Value("${connector.consumer.db-threads:16}")
    private int dbThreads;

    /** Макс одновременно выполняемых батчей к БД */
    @Value("${connector.consumer.max-inflight:64}")
    private int maxInflight;

    /** Вместимость очереди задач к БД (bounded) */
    @Value("${connector.consumer.db-queue-capacity:256}")
    private int dbQueueCapacity;

    /** Максимум записей, которые сливаем из одного poll в память */
    @Value("${connector.consumer.max-merge-records:50000}")
    private int maxMergeRecords;

    /** Размер одного чанка к БД (upsertBatch по chunkSize) — ИСПОЛЬЗУЕМ ключ db-chunk-size */
    @Value("${connector.consumer.db-chunk-size:1000}")
    private int dbChunkSize;

    /** Интервал poll (мс) */
    @Value("${connector.consumer.poll-ms:200}")
    private long pollMs;

    /** Коалесинг: ждать до N мс, чтобы собрать побольше (0 — выключено) */
    @Value("${connector.consumer.coalesce-ms:0}")
    private long coalesceMs;

    /** Порог паузы/резюма */
    @Value("${connector.consumer.pause-threshold-ratio:0.85}")
    private double pauseThresholdRatio;

    /** Гистерезис для resume (меньше дёрганий) */
    @Value("${connector.consumer.resume-threshold-ratio:0.40}")
    private double resumeThresholdRatio;

    // --- исполнители и служебка ---
    private Thread consumerThread;
    private ThreadPoolExecutor dbExecutor;

    private final AtomicReference<KafkaConsumer<String, AccountBatch>> currentConsumer = new AtomicReference<>();

    private final AtomicInteger inflightBatches = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Map<TopicPartition, OffsetAndMetadata>> pendingCommits = new ConcurrentLinkedQueue<>();

    // агрегаты (минимум контеншена)
    private static final LongAdder totalUsers = new LongAdder();
    private static final LongAdder totalBatches = new LongAdder();
    private static final LongAdder totalKafkaRecords = new LongAdder();

    public void start() {
        if (maxInflight <= 0) maxInflight = Math.max(32, dbThreads * 4);
        if (dbQueueCapacity <= 0) dbQueueCapacity = Math.max(128, dbThreads * 16);
        if (dbChunkSize <= 0) dbChunkSize = 1000;
        if (maxMergeRecords < dbChunkSize) maxMergeRecords = dbChunkSize;

        dbExecutor = new ThreadPoolExecutor(
                dbThreads, dbThreads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(dbQueueCapacity),
                r -> {
                    Thread t = new Thread(r, "AccountDB-" + THREAD_ID.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // мягкий бэкпрешур
        );

        log.info("[INIT] AccountConsumer dbThreads={}, dbQueueCap={}, maxInflight={}, pollMs={}, dbChunkSize={}, maxMerge={}, coalesceMs={}",
                dbThreads, dbQueueCapacity, maxInflight, pollMs, dbChunkSize, maxMergeRecords, coalesceMs);

        consumerThread = new Thread(this::consumeLoop, "User-Kafka-Consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("[INIT] Kafka consumer thread started");
    }

    @PreDestroy
    public void shutdown() {
        log.info("[SHUTDOWN] AccountConsumer stopping...");
        KafkaConsumer<String, AccountBatch> c = currentConsumer.get();
        if (c != null) {
            try { c.wakeup(); } catch (Exception ignored) {}
        }
        if (dbExecutor != null) {
            dbExecutor.shutdown();
            try {
                if (!dbExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    dbExecutor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                dbExecutor.shutdownNow();
            }
        }
        log.info("[SHUTDOWN] executor shutdown done");
    }

    private void consumeLoop() {
        try (KafkaConsumer<String, AccountBatch> consumer = consumerProvider.getObject()) {
            currentConsumer.set(consumer);
            consumer.subscribe(Collections.singletonList(usersTopic));
            log.info("[SUBSCRIBE] {}", usersTopic);

            while (true) {
                try {
                    // 1) бэкпрешур до poll()
                    maybePauseOrResume(consumer);

                    // 2) быстрый poll (с опциональным коалесингом)
                    ConsumerRecords<String, AccountBatch> records = consumer.poll(Duration.ofMillis(pollMs));
                    if (coalesceMs > 0 && !records.isEmpty()) {
                        ConsumerRecords<String, AccountBatch> extra = consumer.poll(Duration.ofMillis(coalesceMs));
                        if (!extra.isEmpty()) {
                            records = concat(records, extra);
                        }
                    }

                    // 3) дрейним готовые коммиты (после предыдущих батчей)
                    flushPendingCommits(consumer);

                    if (records.isEmpty()) {
                        continue;
                    }

                    var span = perf.start("AccountConsumer.poll", Map.of(
                            "thread", Thread.currentThread().getName(),
                            "count", String.valueOf(records.count())
                    ));
                    totalKafkaRecords.add(records.count());

                    // 4) собираем пользователей с верхним лимитом (защита от OOM/гигантских poll)
                    List<AccountDto> merged = new ArrayList<>(Math.min(maxMergeRecords, records.count() * 64));
                    for (ConsumerRecord<String, AccountBatch> r : records) {
                        AccountBatch batch = r.value();
                        if (batch != null && batch.getUsers() != null && !batch.getUsers().isEmpty()) {
                            List<AccountDto> u = batch.getUsers();
                            int remaining = maxMergeRecords - merged.size();
                            if (remaining <= 0) break;
                            if (u.size() <= remaining) {
                                merged.addAll(u);
                            } else {
                                merged.addAll(u.subList(0, remaining));
                                break; // достигли лимита
                            }
                        }
                    }
                    span.lap("merge_done");

                    // 5) хвостовые оффсеты для коммита
                    Map<TopicPartition, OffsetAndMetadata> commitMap = tailOffsets(records);

                    if (merged.isEmpty()) {
                        pendingCommits.add(commitMap);
                        flushPendingCommits(consumer);
                        span.finish("no_data", Map.of("queue", String.valueOf(dbExecutor.getQueue().size())));
                        continue;
                    }

                    // 6) режем merged на чанки dbChunkSize и отправляем в пул
                    List<List<AccountDto>> chunks = chunksOf(merged, dbChunkSize);
                    CountDownLatch latch = new CountDownLatch(chunks.size());

                    inflightBatches.addAndGet(chunks.size());
                    totalBatches.add(chunks.size());
                    totalUsers.add(merged.size());

                    for (List<AccountDto> chunk : chunks) {
                        dbExecutor.submit(() -> {
                            var dbSpan = perf.start("AccountConsumer.upsertChunk", Map.of(
                                    "size", String.valueOf(chunk.size())
                            ));
                            try {
                                // Под него: внутри upsertBatch — два шага (UPDATE→INSERT), без MERGE/ON CONFLICT
                                accountService.upsertBatch(chunk);
                                dbSpan.finish("ok", null);
                            } catch (Exception e) {
                                dbSpan.finish("error", Map.of("err", safe(e.getMessage())));
                                log.error("[DB-ERR] upsertBatch chunk size={} failed", chunk.size(), e);
                                // offset не коммитим; весь полл будет переигран
                                throw e;
                            } finally {
                                inflightBatches.decrementAndGet();
                                latch.countDown();
                            }
                        });
                    }

                    // 7) ждём завершения всех чанков этой порции
                    boolean ok = latch.await(5, TimeUnit.MINUTES); // safety-таймаут
                    if (!ok) {
                        log.warn("[TIMEOUT] DB chunks not finished in time; offsets NOT committed");
                        // не коммитим offsets — пускай переиграется
                    } else {
                        // всё успешно — готовим коммит
                        pendingCommits.add(commitMap);
                        flushPendingCommits(consumer);
                    }

                    // 8) сводная строка с периодичностью
                    if (totalBatches.longValue() % 50 == 0) {
                        log.info("[STATS] batches={}, users={}, inflight={}, dbQueue={}, kafkaRecs={}",
                                totalBatches.longValue(), totalUsers.longValue(),
                                inflightBatches.get(), dbExecutor.getQueue().size(), totalKafkaRecords.longValue());
                    }

                    span.finish("done", Map.of(
                            "merged", String.valueOf(merged.size()),
                            "chunks", String.valueOf(chunks.size()),
                            "inflight", String.valueOf(inflightBatches.get()),
                            "dbQueue", String.valueOf(dbExecutor.getQueue().size())
                    ));

                } catch (WakeupException e) {
                    log.info("[WAKEUP] Stop signal");
                    break;
                } catch (Exception e) {
                    log.error("[ERROR] AccountConsumer loop", e);
                }
            }
        } finally {
            currentConsumer.set(null);
            log.info("[CLOSE] Kafka consumer closed");
        }
    }

    /** Конкатенация двух ConsumerRecords (простейшая). */
    private static ConsumerRecords<String, AccountBatch> concat(ConsumerRecords<String, AccountBatch> a,
                                                                ConsumerRecords<String, AccountBatch> b) {
        Map<TopicPartition, List<ConsumerRecord<String, AccountBatch>>> map = new HashMap<>(a.partitions().size() + b.partitions().size());
        for (TopicPartition tp : a.partitions()) map.put(tp, new ArrayList<>(a.records(tp)));
        for (TopicPartition tp : b.partitions()) map.computeIfAbsent(tp, k -> new ArrayList<>()).addAll(b.records(tp));
        return new ConsumerRecords<>(map);
    }

    /** Последние оффсеты по каждой партиции */
    private static Map<TopicPartition, OffsetAndMetadata> tailOffsets(ConsumerRecords<String, AccountBatch> records) {
        Map<TopicPartition, OffsetAndMetadata> commitMap = new HashMap<>();
        for (TopicPartition tp : records.partitions()) {
            List<ConsumerRecord<String, AccountBatch>> lst = records.records(tp);
            if (!lst.isEmpty()) {
                long last = lst.get(lst.size() - 1).offset();
                commitMap.put(tp, new OffsetAndMetadata(last + 1));
            }
        }
        return commitMap;
    }

    /** Коммитим все готовые оффсеты (в consumer-потоке), берём максимум по каждой партиции. */
    private void flushPendingCommits(KafkaConsumer<String, AccountBatch> consumer) {
        Map<TopicPartition, OffsetAndMetadata> toCommit = new HashMap<>();
        for (;;) {
            Map<TopicPartition, OffsetAndMetadata> m = pendingCommits.poll();
            if (m == null) break;
            for (var e : m.entrySet()) {
                toCommit.merge(e.getKey(), e.getValue(), (a, b) -> (b.offset() > a.offset()) ? b : a);
            }
        }
        if (!toCommit.isEmpty()) {
            try {
                consumer.commitSync(toCommit);
            } catch (CommitFailedException cfe) {
                log.warn("[COMMIT] failed: {}", cfe.toString(), cfe);
            }
        }
    }

    /** Бэкпрешур: учитываем и очередь, и inflight; гистерезис, чтобы не дёргать лишний раз. */
    private void maybePauseOrResume(KafkaConsumer<String, AccountBatch> consumer) {
        int q = dbExecutor.getQueue().size();
        int inflight = inflightBatches.get();

        int pauseQ = (int) (dbQueueCapacity * pauseThresholdRatio);
        int resumeQ = (int) (dbQueueCapacity * resumeThresholdRatio);

        Set<TopicPartition> assigned = consumer.assignment();
        if (assigned == null || assigned.isEmpty()) return;

        boolean paused = isPaused(consumer, assigned);

        if (!paused && (q > pauseQ || inflight > maxInflight)) {
            consumer.pause(assigned);
            log.debug("[PAUSE] q={}, inflight={}", q, inflight);
        } else if (paused && (q < resumeQ && inflight < (int)(maxInflight * 0.7))) {
            consumer.resume(assigned);
            log.debug("[RESUME] q={}, inflight={}", q, inflight);
        }
    }

    private boolean isPaused(KafkaConsumer<String, AccountBatch> consumer, Set<TopicPartition> assigned) {
        try {
            return consumer.paused().containsAll(assigned);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static List<List<AccountDto>> chunksOf(List<AccountDto> all, int size) {
        int n = all.size();
        if (n <= size) return List.of(all);
        List<List<AccountDto>> res = new ArrayList<>((n + size - 1) / size);
        for (int i = 0; i < n; i += size) {
            res.add(all.subList(i, Math.min(n, i + size)));
        }
        return res;
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
}
