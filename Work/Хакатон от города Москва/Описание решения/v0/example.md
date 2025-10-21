# Архитектура обработки данных и принятия решений

## 0) Исходные точки (как «пришло» с объекта)

**Вход** (локальное время Europe/Warsaw; для шины всё будет в UTC):

| ts (лок.) | entity   | signal          | value | unit | quality.valid | source |
|---|---|---|---:|---|---:|---|
| 10:00 | PS-0134 | pressure_inlet | 3.70 | bar | 1 | opc |
| 10:01 | PS-0134 | pressure_inlet | 3.64 | bar | 1 | opc |
| 10:02 | PS-0134 | pressure_inlet | 3.58 | bar | 1 | opc |
| 10:03 | PS-0134 | pressure_inlet | 3.52 | bar | 1 | opc |
| 10:04 | PS-0134 | pressure_inlet | 3.46 | bar | 1 | opc |
| 10:05 | PS-0134 | pressure_inlet | 3.40 | bar | 1 | opc |
| 10:00 | Z5      | flow_out       |  950 | m³/h | 1 | opc |
| 10:01 | Z5      | flow_out       | 1010 | m³/h | 1 | opc |
| 10:02 | Z5      | flow_out       | 1090 | m³/h | 1 | opc |
| 10:03 | Z5      | flow_out       | 1180 | m³/h | 1 | opc |
| 10:04 | Z5      | flow_out       | 1320 | m³/h | 1 | opc |
| 10:05 | Z5      | flow_out       | 1480 | m³/h | 1 | opc |
| 10:05 | PS-0134 | pump_rpm       | 0.86 | 1    | 1 | opc |

**Почему показываем:** это «истина». Отсюда строятся все агрегаты/признаки и аудит качества.

---

## 1) Ingest Gate → `telemetry.raw.v1`

**Вход:** таблица выше.  
**Что делает и зачем:**  
- Приводит время к **UTC** (локальное 10:05 ⇒ **08:05Z**), единицы — к каталогу.  
- Ставит **флаги качества** (`range/valid/stale/confidence`) по **Signal Catalog**.  
- Пишет надёжно: **Idempotency + WAL** (не будет дублей, выдержим отвал шины).  
- Отправляет в Kafka с **ключом партиции = entity** (сохраняет порядок внутри объекта).

**Выход** (фрагмент `telemetry.raw.v1`):

| ts_utc | entity | signal | value | unit | quality.range | quality.valid | quality.stale | quality.confidence | source | partition_key |
|---|---|---|---:|---|---|---:|---:|---:|---|---|
| 08:05:00Z | PS-0134 | pressure_inlet | 3.40 | bar | soft_violation | 1 | 0 | 0.86 | opc | PS-0134 |
| 08:05:00Z | Z5 | flow_out | 1480 | m³/h | ok | 1 | 0 | 0.95 | opc | Z5 |
| 08:05:00Z | PS-0134 | pump_rpm | 0.86 | 1 | ok | 1 | 0 | 0.95 | opc | PS-0134 |

**Термины:**  
- **range:** сравнение со **soft/hard** пределами из каталога (мягкое/жёсткое нарушение).  
- **valid:** пригодно для анализа (жёсткое нарушение → 0).  
- **stale:** опоздала или сенсор «молчит».  
- **confidence:** «насколько доверяем измерению» (0..1), база по источнику × штрафы.

---

## 2) Stream Processor → `telemetry.agg.v1`, `data_quality.events.v1`

**Вход:** `telemetry.raw.v1`.  
**Что делает и зачем:**  
- Считает **окна** (1/5/15 мин): `mean` (среднее), `stddev` (разброс), `slope` (наклон, ед./мин), `zscore_last` (последняя точка против окна).  
- Проверяет **DQ** (качество данных): completeness/freshness/outlier/sticking. Это отделяет «сбой датчика» от «процессного события».

**Выход** (окно **5 минут**, 10:01–10:05 ⇒ 08:01–08:05Z):

| entity | signal | window | window_end_ts_utc | count | min | max | mean | stddev | slope | zscore_last | quality.complete_ratio |
|---|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| PS-0134 | pressure_inlet | 5m | 08:05:00Z | 5 | 3.40 | 3.64 | **3.52** | 0.0949 | **-0.06** | — | 0.98 |
| Z5 | flow_out | 5m | 08:05:00Z | 5 | 1010 | 1480 | **1216** | 187.16 | — | **1.41** | 0.98 |

**DQ события (пример структуры):**

| entity | signal | dq_status | dq_detail | value | note |
|---|---|---|---|---|---|
| PS-0134 | pressure_inlet | ok | — | — | freshness_ok |
| Z5 | flow_out | ok | — | — | freshness_ok |

**Термины:**  
- **slope:** скорость изменения за окно (линейная регрессия), ед./мин.  
- **zscore_last:** насколько последняя точка окна отличается от среднего в σ.  
- **completeness:** доля полученных точек в окне (важно для доверия метрикам).

---

## 3) Feature Service → **baseline** и онлайн-признаки

**Вход:** AGG + история (30 дней) для baseline.  
**Что делает и зачем:**  
- Строит **baseline по часу недели** (168 норм) для каждого `(entity, signal)` → «ожидаемое» значение в этот час.  
- Отдаёт **ratio_to_baseline** и **delta_to_baseline** — универсальные признаки «насколько выше/ниже нормы».

**Baseline** (фрагмент):

| entity | signal | granularity | stat | baseline_value |
|---|---|---|---|---:|
| Z5 | flow_out | hour_of_week | median_30d | **820.0** |
| PS-0134 | pressure_inlet | hour_of_week | median_30d | **3.9** |

**Онлайн-фичи (на 08:05Z):**

| entity | ts_utc | mean_5m | slope_5m | delta_to_baseline | ratio_to_baseline |
|---|---|---:|---:|---:|---:|
| PS-0134 | 08:05:00Z | 3.52 | -0.06 | **-0.38** | — |
| Z5 | 08:05:00Z | 1216 | — | — | **1.48** |

**Термины:**  
- **baseline:** медиана по этому часу недели за 30 дней (можно хранить ещё p25/p75).  
- **ratio_to_baseline:** mean_5m / baseline (1.48 ⇒ выше нормы на 48%).  
- **delta_to_baseline:** mean_5m − baseline (−0.38 бар ⇒ ниже нормы).

---

## 4) CEP + Rules → `rules.findings.v1` (паттерн R-LEAK-01)

**Вход:** AGG/DQ + политика YAML.  
**Что делает и зачем:**  
- Проверяет **комбинацию условий** (ALL/ANY/WITHIN/SEQUENCE) с **persistence** ⇒ объяснимый факт «технологической ситуации».  
- Горячая подмена YAML-правил (Git-ops), версии, области действия.

**Оценка условий:**

| signal | metric | op | threshold | observed | pass |
|---|---|---|---:|---:|:--:|
| pressure_inlet | slope_5m | < | -0.05 | **-0.06** | ✅ |
| pressure_inlet | mean_5m | < | 3.50 | **3.52** | ❌ *(на грани; можно задать порог 3.55)* |
| flow_out | ratio_to_baseline | > | 1.30 | **1.48** | ✅ |
| flow_out | zscore_last | > | 2.50 | **1.41** | ❌ |

**Факт (сработает, если учтём persistence и порог mean=3.55):**

| fact_id | rule | asset_id | zone | severity | fired | evidence |
|---|---|---|---|---|:--:|---|
| F-LEAK-01/PS-0134@08:05 | R-LEAK-01@v1 | PS-0134 | Z5 | high | ✅ | p_slope_5m=-0.06; p_mean_5m=3.52; flow_ratio_5m=1.48 |

**Термины:**  
- **ALL/ANY/WITHIN/SEQUENCE** — логика сочетаний и временные окна.  
- **persistence:** условие должно держаться ≥ N секунд (отсечь короткие «пшики»).

---

## 5) ML Inference (gRPC) → скор и прогноз

**Вход (фичи):**

| p_inlet_mean_5m | p_inlet_slope_5m | flow_ratio_5m | flow_z_5m | hour_of_week | rpm_mean_1m |
|---:|---:|---:|---:|---:|---:|
| 3.52 | -0.06 | 1.48 | 1.41 | 130 | 0.86 |

**Выход:**

| anomaly_score | forecast_p_inlet_t+10m | forecast_ci95_low | forecast_ci95_high | explain |
|---:|---:|---:|---:|---|
| **0.82** | **3.10** | 3.0 | 3.3 | p_inlet_slope_5m; flow_ratio_5m |

**Термины:**  
- **anomaly_score (0..1):** насколько ситуация не похожа на «норму».  
- **forecast:** краткосрочный прогноз (горизонт 10 мин) + доверительный интервал.  
- **explain:** ведущие признаки (SHAP/Permutation).

---

## 6) Digital Twin (REST) → проверка действия

**Вход (сценарий):**

| scenario |
|---|
| pump_rpm_delta = -0.15; open_valve_ZD12 = +5% |

**Выход:**

| what_if_id | pressure_inlet_delta | stabilization_sec |
|---|---|---:|
| WIF-92B3 | +0.5 bar | **480** |

**Термины:**  
- **what-if:** симуляция «что будет, если выполнить действие».  
- **stabilization_sec:** время выхода на новый режим.

---

## 7) Recommendation Engine → `reco.events.v1` (карточка)

**Вход (агрегация уверенности):**

| s_rules | s_ml | s_twin | s_stat | w_rules | w_ml | w_twin | w_stat | bias | logit_u | confidence |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 0.90 | 0.82 | 0.80 | 0.70 | 1.3 | 0.8 | 1.1 | 0.5 | -0.5 | 2.170 | **0.90** |

**Выход (карточка):**

| id | asset_id | generated_at | severity | confidence | title | action | window_sec | explain_rule | explain_ml | explain_twin |
|---|---|---|---|---:|---|---|---:|---|---|---|
| REC-7F21 | PS-0134 | 08:05:24Z | high | **0.90** | Риск утечки (падение P + рост Flow) | Уменьшить RPM N1 на 10–15% и приоткрыть ЗД-12 на +5% | **180** | R-LEAK-01: slope=-0.06; ratio=1.48 | anomaly=0.82; P(t+10m)=3.10 [3.0;3.3] | RPM −15% → +0.5 бар за 8 мин |

**Термины:**  
- **confidence:** объединение доказательств (rules/ml/twin/stat) по весам через логистическую функцию.  
- **severity:** шкала приоритета, пороги настраиваются (например, high ≥ 0.8).

---

## 8) Kafka-топики и читатели

| topic | produced_by | consumed_by |
|---|---|---|
| telemetry.raw.v1 | Ingest Gate | Stream Proc, Timescale (архив RAW) |
| telemetry.agg.v1 | Stream Proc | CEP+Rules, FeatureSvc, UI |
| data_quality.events.v1 | Stream Proc | CEP+Rules, Monitoring |
| rules.findings.v1 | CEP+Rules | Recommendation Engine |
| reco.events.v1 | Recommendation Engine | UI/Workflow |

**Почему так:** ключ партиции = `entity` сохраняет порядок по объекту; разные **consumer groups** масштабируются независимо.

---

## 9) Хранилища: что куда пишем

| store | table | what |
|---|---|---|
| TimescaleDB | telemetry | RAW точки (UTC, нормализованные) |
| TimescaleDB | telemetry_agg_5m (cont. agg) | mean/slope/z для UI |
| Postgres | policies | YAML-правила с версиями |
| Postgres | reco_log | события-карточки |
| Postgres | feedback | ACK/полезно/комментарии |
| Lakehouse/ClickHouse | hist_telemetry | длинная история/обучение |

---

## Короткие расшифровки терминов по ходу

- **Idempotency:** повтор того же события **не меняет** результата; ключ — `(entity, signal, ts, source)`.  
- **WAL (Write-Ahead Log):** сперва на диск, потом в Kafka; после аварии — «реплей» без потерь/дублей.  
- **Baseline (hour_of_week):** динамическая норма «для этого часа», 168 значений/тег.  
- **slope:** скорость изменения за окно (ед./мин).  
- **z-score:** «во сколько σ» точка отличается от среднего окна.  
- **persistence:** минимальная длительность выполнения условий.  
- **anomaly_score:** 0..1, как «не похоже на норму».  
- **what-if / twin:** расчёт на гидромодели «что будет, если…».  
- **confidence:** объединённая уверенность (rules/ml/twin/stat) → 0..1.

