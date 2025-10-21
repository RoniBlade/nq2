from typing import Optional
import pandas as pd
from sqlalchemy import create_engine, text

class PgStore:
    def __init__(self, dsn: str):
        self.engine = create_engine(dsn, pool_pre_ping=True)

    def append(self, sensor_id: str, df_new: pd.DataFrame):
        if df_new.empty: return
        df_new = df_new.copy()
        df_new["is_weekend"] = df_new["is_weekend"].astype(bool)
        # upsert через временную таблицу
        with self.engine.begin() as cx:
            df_new.to_sql("_tmp_ds", cx, if_exists="replace", index=False)
            cx.execute(text("""
                INSERT INTO datasets_hourly (sensor_id, ts, hour, is_weekend, consumption, t1, t2, supply, return)
                SELECT sensor_id, ts, hour, is_weekend, consumption, t1, t2, supply, "return"
                FROM _tmp_ds
                ON CONFLICT (sensor_id, ts) DO UPDATE
                SET hour=EXCLUDED.hour, is_weekend=EXCLUDED.is_weekend,
                    consumption=EXCLUDED.consumption, t1=EXCLUDED.t1, t2=EXCLUDED.t2,
                    supply=EXCLUDED.supply, return=EXCLUDED.return;
                DROP TABLE _tmp_ds;
            """))

    def load_all(self, sensor_id: str) -> pd.DataFrame:
        q = """
        SELECT sensor_id, ts, EXTRACT(HOUR FROM ts) AS hour,
               CASE WHEN EXTRACT(DOW FROM ts) IN (6,0) THEN 1 ELSE 0 END AS is_weekend,
               consumption, t1, t2, supply, "return"
        FROM datasets_hourly
        WHERE sensor_id=:sid
        ORDER BY ts
        """
        with self.engine.begin() as cx:
            return pd.read_sql(text(q), cx, params={"sid": sensor_id})

    def max_ts(self, sensor_id: str) -> Optional[int]:
        q = "SELECT MAX(EXTRACT(EPOCH FROM ts))*1000 AS ts_ms FROM datasets_hourly WHERE sensor_id=:sid"
        with self.engine.begin() as cx:
            r = cx.execute(text(q), {"sid": sensor_id}).scalar()
            return int(r) if r else None
