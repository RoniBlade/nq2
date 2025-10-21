CREATE TABLE IF NOT EXISTS aggregates_hourly (
  id BIGSERIAL PRIMARY KEY,
  sensor_id TEXT NOT NULL,
  ts_hour TIMESTAMPTZ NOT NULL,
  hour_of_day INT NOT NULL,
  is_weekend BOOLEAN NOT NULL,
  x_sum DOUBLE PRECISION,
  x_mean DOUBLE PRECISION,
  x_min DOUBLE PRECISION,
  x_max DOUBLE PRECISION,
  x_count INT,
  dt_mean DOUBLE PRECISION,
  dt_count INT,
  CONSTRAINT uq_agg_sensor_ts UNIQUE(sensor_id, ts_hour)
);
CREATE INDEX IF NOT EXISTS idx_agg_slot ON aggregates_hourly(sensor_id, hour_of_day, is_weekend, ts_hour DESC);

CREATE TABLE IF NOT EXISTS rule_params (
  id BIGSERIAL PRIMARY KEY,
  sensor_id TEXT NOT NULL,
  hour_of_day INT NOT NULL,
  is_weekend BOOLEAN NOT NULL,
  payload JSONB NOT NULL,
  generated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_rule_slot UNIQUE(sensor_id, hour_of_day, is_weekend)
);
CREATE INDEX IF NOT EXISTS idx_rule_slot ON rule_params(sensor_id, hour_of_day, is_weekend);

CREATE TABLE IF NOT EXISTS alerts_stat (
  id BIGSERIAL PRIMARY KEY,
  sensor_id TEXT NOT NULL,
  ts TIMESTAMPTZ NOT NULL,
  severity TEXT NOT NULL,
  x DOUBLE PRECISION,
  dt DOUBLE PRECISION,
  median DOUBLE PRECISION,
  upper DOUBLE PRECISION,
  z DOUBLE PRECISION,
  reason TEXT
);
CREATE INDEX IF NOT EXISTS idx_alerts_ts ON alerts_stat(sensor_id, ts DESC);
