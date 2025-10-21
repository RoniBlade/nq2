import os
import time
import yaml
import logging
from typing import Dict, List, Tuple
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sklearn.ensemble import RandomForestRegressor
from sklearn.multioutput import MultiOutputRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

from trainer_rest.kafka_io import build_consumer, build_producer, read_stream, publish_event
from trainer_rest.dataset_builder import build_datasets_from_messages
from trainer_rest.datastore_parquet import DataStoreParquet
from trainer_rest.model_registry import ModelRegistry
from trainer_rest.features import build_features

logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("trainer")

# ---------------- CONFIG ----------------
CONFIG_PATH = "config.yaml"

def load_cfg():
    if not os.path.exists(CONFIG_PATH):
        return {
            "kafka": {
                "bootstrap": "localhost:9092",
                "group_id": "trainer-rest",
                "topics": ["HVS_RAW_DATA", "ODPU_RAW_DATA"],
                "model_events_topic": "models.events"
            },
            "features": {"lags": 24, "rolling_windows": [6, 12, 24]},
            "model": {"type": "random_forest", "params": {"n_estimators": 200, "max_depth": 12, "n_jobs": -1}},
            "min_rows_per_sensor": 50,
            "models": {"baseDir": "./models"},
            "datasets": {"baseDir": "./datasets"}
        }
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)

CFG = load_cfg()

def extract_topics(cfg) -> List[str]:
    k = cfg.get("kafka") or cfg.get("app", {}).get("kafka") or {}
    topics = k.get("topics", [])
    if isinstance(topics, dict):
        flat = []
        for v in topics.values():
            if isinstance(v, dict):
                flat += [vv for vv in v.values() if isinstance(vv, str)]
            elif isinstance(v, list):
                flat += [x for x in v if isinstance(x, str)]
            elif isinstance(v, str):
                flat.append(v)
        return flat
    if isinstance(topics, list):
        return topics
    return []

KAFKA = (CFG.get("kafka") or CFG.get("app", {}).get("kafka", {}))
KAFKA_BOOTSTRAP = KAFKA.get("bootstrap", "localhost:9092")
KAFKA_GROUP_ID  = KAFKA.get("group_id", "trainer-rest")
TOPICS = extract_topics(CFG)
MODEL_EVENTS_TOPIC = KAFKA.get("model_events_topic") or (KAFKA.get("topics", {}).get("out", {}).get("models", "models.events"))

MODELS_DIR   = CFG.get("models", {}).get("baseDir", "./models")
DATASETS_DIR = CFG.get("datasets", {}).get("baseDir", "./datasets")
os.makedirs(MODELS_DIR, exist_ok=True)
os.makedirs(DATASETS_DIR, exist_ok=True)

# ---------------- APP ----------------
app = FastAPI(title="Trainer + Infer (topic == sensor)", version="7.0")

store    = DataStoreParquet(DATASETS_DIR)
registry = ModelRegistry(MODELS_DIR)

class TrainRequest(BaseModel):
    seconds: int = 20  # сколько слушаем кафку

class PredictRequest(BaseModel):
    minutes: int = 60  # горизонт возвращаем в ответе

# ---------------- TRAIN ----------------
@app.post("/train")
def train(req: TrainRequest):
    secs = max(2, min(180, req.seconds))
    log.info(f"[TRAIN] start, topics={TOPICS}, seconds={secs}")

    consumer = build_consumer(
        bootstrap=KAFKA_BOOTSTRAP,
        group_id=f"{KAFKA_GROUP_ID}-{int(time.time())}",
        topics=TOPICS
    )
    stream = read_stream(consumer)  # -> yields (topic, message_dict) | None
    messages: List[Tuple[str, dict]] = []

    t0 = time.time()
    empty = 0
    while time.time() - t0 < secs:
        item = next(stream)
        if item is None:
            empty += 1
            if empty > 20:
                break
            continue
        empty = 0
        messages.append(item)

    consumer.close()
    log.info(f"[KAFKA] collected {len(messages)} messages")

    if not messages:
        raise HTTPException(status_code=400, detail="Нет данных из Kafka")

    # строим ОТДЕЛЬНЫЕ датасеты: topic == sensor_id
    sensor_dfs: Dict[str, pd.DataFrame] = build_datasets_from_messages(messages)
    results = []

    for sensor_id, df_sensor in sensor_dfs.items():
        # сохранить историю сенсора (пополним parquet)
        store.append(sensor_id, df_sensor)
        full_df = store.load_all(sensor_id)
        log.info(f"[TRAIN] sensor={sensor_id}, dataset size={len(full_df)}")

        # таргеты = все numeric-поля из flow (они уже лежат как колонки)
        base_ignore = {"sensor_id", "ts", "hour", "is_weekend"}
        target_cols = [c for c in full_df.select_dtypes(include=["number"]).columns if c not in base_ignore]

        if not target_cols:
            log.info(f"[TRAIN] skip {sensor_id}, нет числовых полей из flow")
            continue

        # построить фичи ровно по этим колонкам
        X, y, feature_cols, meta = build_features(full_df, CFG, target_cols=target_cols)
        if len(X) < CFG.get("min_rows_per_sensor", 50):
            log.info(f"[TRAIN] skip {sensor_id}, too few rows after features: {len(X)}")
            continue

        split = int(len(X) * 0.8)
        Xtr, Xte = X.iloc[:split], X.iloc[split:]
        ytr, yte = y.iloc[:split], y.iloc[split:]

        model = MultiOutputRegressor(RandomForestRegressor(**CFG["model"]["params"]))
        log.info(f"[MODEL] train RF(MO) sensor={sensor_id} rows={len(Xtr)} features={len(feature_cols)} targets={len(target_cols)}")
        model.fit(Xtr, ytr)

        yhat = model.predict(Xte) if len(Xte) else model.predict(Xtr)
        ytrue = yte if len(Xte) else ytr

        mae  = float(mean_absolute_error(ytrue, yhat))
        rmse = float(mean_squared_error(ytrue, yhat, squared=False))
        try:
            r2 = float(r2_score(ytrue, yhat))
        except Exception:
            r2 = float("nan")

        version = registry.save(sensor_id, model, feature_cols, list(y.columns))
        log.info(f"[MODEL] trained {sensor_id}: mae={mae:.4f}, rmse={rmse:.4f}, r2={r2:.3f}")

        payload = {
            "eventType": "MODEL_TRAINED",
            "sensorId": sensor_id,
            "version": version,
            "targets": list(y.columns),
            "metrics": {"mae": mae, "rmse": rmse, "r2": r2},
            "timestamp": int(time.time() * 1000)
        }
        try:
            prod = build_producer(KAFKA_BOOTSTRAP)
            publish_event(prod, MODEL_EVENTS_TOPIC, payload)
        except Exception as e:
            log.info(f"[EVENT][WARN] publish failed: {e}")

        results.append(payload)

    return {"status": "ok", "trained": results}

# ---------------- PREDICT ----------------
@app.post("/predict/{sensor_id}")
def predict(sensor_id: str, req: PredictRequest):
    try:
        model, features, targets, version = registry.load(sensor_id)
        log.info(f"[PREDICT] loaded model sensor={sensor_id}, version={version}")
    except Exception:
        raise HTTPException(status_code=404, detail=f"Model for {sensor_id} not found")

    df = store.load_all(sensor_id)
    if df.empty:
        raise HTTPException(status_code=400, detail=f"No data for {sensor_id}")

    # строим фичи ТАК ЖЕ, как при обучении
    X, _, feat_cols_now, meta = build_features(df, CFG, target_cols=targets)
    if X.empty:
        raise HTTPException(status_code=400, detail="No features built (not enough history)")

    last = X.iloc[-1]
    xx = last.reindex(features).fillna(0.0).to_numpy().reshape(1, -1)

    pred = model.predict(xx)[0].tolist()
    return {
        "sensorId": sensor_id,
        "version": version,
        "horizon": req.minutes,
        "prediction": dict(zip(targets, pred))
    }

@app.get("/health")
def health():
    return {"status": "ok"}
