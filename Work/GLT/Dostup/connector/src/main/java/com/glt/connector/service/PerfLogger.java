package com.glt.connector.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Перф-логгер: пишет СТРОГО JSON-строку в логгер "perf".
 * Пример:
 *   var span = perf.start("ScanTaskWorker", Map.of("taskId", taskId, "ip", ip));
 *   span.lap("scanUsers_start");
 *   ...
 *   span.finish("ok", Map.of("users","123"));
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PerfLogger {

    private static final Logger PERF = LoggerFactory.getLogger("perf");

    /** Запустить спан */
    public PerfSpan start(@NonNull String component, Map<String, String> ctx) {
        return new PerfSpan(component, ctx == null ? Map.of() : ctx);
    }

    /** Отдельный «спан» с фазами и аннотациями. Потокобезопасно в одном потоке. */
    public static final class PerfSpan {
        private final String component;
        private final Map<String, String> ctx;
        private final long t0;
        private long last;
        private final List<String> marks = new ArrayList<>();
        private final Map<String, String> annotations = new LinkedHashMap<>();

        private PerfSpan(String component, Map<String, String> ctx) {
            this.component = component;
            this.ctx = ctx;
            this.t0 = System.nanoTime();
            this.last = t0;
        }

        /** Засечь фазу */
        public PerfSpan lap(@NonNull String phase) {
            long now = System.nanoTime();
            long ms = (now - last) / 1_000_000;
            last = now;
            marks.add("\"" + phase + "\":" + ms);
            return this;
        }

        /** Добавить произвольные поля-объекты (например, sizes/errors) */
        public PerfSpan annotate(@NonNull String key, @NonNull Map<String, String> payload) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (var e : payload.entrySet()) {
                if (!first) sb.append(',');
                sb.append("\"").append(e.getKey()).append("\":\"").append(safe(e.getValue())).append("\"");
                first = false;
            }
            sb.append("}");
            annotations.put(key, sb.toString());
            return this;
        }

        /** Закончить и записать строку JSON в логгер "perf" */
        public void finish(@NonNull String outcome, Map<String, String> extra) {
            long totalMs = (System.nanoTime() - t0) / 1_000_000;

            StringBuilder sb = new StringBuilder(512);
            sb.append("{\"component\":\"").append(component).append("\"");

            String traceId = MDC.get("traceId");
            if (traceId != null) sb.append(",\"traceId\":\"").append(safe(traceId)).append("\"");

            if (!ctx.isEmpty()) {
                for (var e : ctx.entrySet()) {
                    sb.append(",\"").append(e.getKey()).append("\":\"").append(safe(e.getValue())).append("\"");
                }
            }
            sb.append(",\"outcome\":\"").append(safe(outcome)).append("\"");
            sb.append(",\"total_ms\":").append(totalMs);

            if (extra != null && !extra.isEmpty()) {
                for (var e : extra.entrySet()) {
                    sb.append(",\"").append(e.getKey()).append("\":\"").append(safe(e.getValue())).append("\"");
                }
            }

            if (!annotations.isEmpty()) {
                for (var e : annotations.entrySet()) {
                    sb.append(",\"").append(e.getKey()).append("\":").append(e.getValue());
                }
            }

            if (!marks.isEmpty()) {
                sb.append(",\"phases\":{");
                for (int i = 0; i < marks.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(marks.get(i));
                }
                sb.append('}');
            }

            sb.append('}');
            PERF.info("{}", sb);
        }

        private static String safe(String v) {
            if (v == null) return "";
            return v.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
