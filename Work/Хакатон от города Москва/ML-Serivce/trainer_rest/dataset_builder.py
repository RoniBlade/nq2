import pandas as pd
from typing import Dict, List, Tuple


def build_datasets_from_messages(messages: List[Tuple[str, dict]]) -> Dict[str, pd.DataFrame]:
    """
    messages: [(topic, message_dict), ...]
    Возвращает dict: sensor_id -> DataFrame с полями из flow
    """
    result: Dict[str, pd.DataFrame] = {}

    for topic, msg in messages:
        sensor_id = msg.get("sensorId") or topic  # берём sensorId из Kafka
        ts = msg.get("timestamp")

        # базовые поля
        row = {
            "sensor_id": sensor_id,
            "ts": ts,
            "hour": pd.to_datetime(ts, unit="ms").hour if ts else None,
            "is_weekend": pd.to_datetime(ts, unit="ms").dayofweek >= 5 if ts else None,
        }

        # все числовые поля из flow
        flow = msg.get("flow", {})
        for k, v in flow.items():
            if isinstance(v, (int, float)):
                row[k] = v

        df = pd.DataFrame([row])

        if sensor_id not in result:
            result[sensor_id] = df
        else:
            result[sensor_id] = pd.concat([result[sensor_id], df], ignore_index=True)

    # сортировка по времени
    for sid in result:
        if not result[sid].empty:
            result[sid] = result[sid].sort_values("ts").reset_index(drop=True)

    return result
