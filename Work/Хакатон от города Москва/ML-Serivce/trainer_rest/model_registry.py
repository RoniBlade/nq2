import os
import json
import time
import joblib

class ModelRegistry:
    def __init__(self, base_dir: str = "./models"):
        self.base_dir = base_dir
        os.makedirs(self.base_dir, exist_ok=True)

    def save(self, sensor_id: str, model, feature_cols, target_cols, version: str | None = None) -> str:
        v = version or str(int(time.time()))
        d = os.path.join(self.base_dir, sensor_id, v)
        os.makedirs(d, exist_ok=True)
        joblib.dump(model, os.path.join(d, "model.joblib"))
        with open(os.path.join(d, "meta.json"), "w", encoding="utf-8") as f:
            json.dump({"version": v, "features": feature_cols, "targets": target_cols}, f, ensure_ascii=False, indent=2)
        print(f"[MODEL_REGISTRY] saved sensor={sensor_id}, version={v}, path={d}")
        return v

    def load(self, sensor_id: str):
        d = os.path.join(self.base_dir, sensor_id)
        if not os.path.isdir(d):
            raise FileNotFoundError(f"No models for sensor {sensor_id}")
        versions = sorted(os.listdir(d))
        v = versions[-1]
        model = joblib.load(os.path.join(d, v, "model.joblib"))
        with open(os.path.join(d, v, "meta.json"), "r", encoding="utf-8") as f:
            meta = json.load(f)
        return model, meta["features"], meta["targets"], meta["version"]
