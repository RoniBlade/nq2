-- История ХВС (часовые батчи)
CREATE TABLE hvs_hour_history (
    id BIGSERIAL PRIMARY KEY,
    sensor_id TEXT NOT NULL,
    asset_id TEXT NOT NULL,
    ts BIGINT NOT NULL,                    -- timestamp в мс
    flow_date TEXT,                        -- как приходит в JSON
    flow_time TEXT,                        -- "0:00", "1:00", ...
    cumulative_consumption DOUBLE PRECISION,
    consumption_for_period DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_hvs_hour_history_sensor_ts ON hvs_hour_history(sensor_id, ts);

-- История ГВС (часовые батчи)
CREATE TABLE gvs_hour_history (
    id BIGSERIAL PRIMARY KEY,
    sensor_id TEXT NOT NULL,
    asset_id TEXT NOT NULL,
    ts BIGINT NOT NULL,
    flow_date TEXT,
    flow_time TEXT,
    supply DOUBLE PRECISION,
    return_val DOUBLE PRECISION,
    consumption_for_period DOUBLE PRECISION,
    t1 DOUBLE PRECISION,
    t2 DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_gvs_hour_history_sensor_ts ON gvs_hour_history(sensor_id, ts);

CREATE TABLE comparisons (
    id BIGSERIAL PRIMARY KEY,
    sensor_id TEXT NOT NULL,
    ts TIMESTAMPTZ NOT NULL,            -- время проверки
    -- входные параметры (например, фактический supply/return/etc)
    input JSONB NOT NULL,
    -- результаты правил
    rule_result JSONB,
    -- результаты ML
    ml_result JSONB,
    -- опционально финальная метка/статус
    status TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_comparisons_sensor_ts ON comparisons(sensor_id, ts);
