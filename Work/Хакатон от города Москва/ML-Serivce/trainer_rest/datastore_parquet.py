import os
import pandas as pd


class DataStoreParquet:
    """
    Хранит данные по каждому сенсору в отдельной папке:
    ./datasets/{sensor_id}/data.parquet
    """

    def __init__(self, base_dir: str = "./datasets"):
        self.base_dir = base_dir
        os.makedirs(self.base_dir, exist_ok=True)

    def _path(self, sensor_id: str) -> str:
        sensor_dir = os.path.join(self.base_dir, sensor_id)
        os.makedirs(sensor_dir, exist_ok=True)
        return os.path.join(sensor_dir, "data.parquet")

    def append(self, sensor_id: str, df_new: pd.DataFrame):
        """Добавить новые данные к истории сенсора"""
        if df_new.empty:
            return
        path = self._path(sensor_id)
        df_new = df_new.sort_values("ts")
        if os.path.exists(path):
            df_old = pd.read_parquet(path)
            df = pd.concat([df_old, df_new], ignore_index=True).drop_duplicates(subset=["ts"])
        else:
            df = df_new
        df = df.sort_values("ts").reset_index(drop=True)
        df.to_parquet(path, index=False)
        print(f"[DATASTORE] sensor={sensor_id}, append {len(df_new)} rows, total={len(df)}")

    def load_all(self, sensor_id: str) -> pd.DataFrame:
        """Загрузить весь датасет сенсора"""
        path = self._path(sensor_id)
        if not os.path.exists(path):
            return pd.DataFrame()
        df = pd.read_parquet(path)
        print(f"[DATASTORE] sensor={sensor_id}, loaded {len(df)} rows")
        return df
