-- Правила для ХВС
CREATE TABLE IF NOT EXISTS rule_params_hvs (
    sensor_id TEXT PRIMARY KEY,
    median_consumption DOUBLE PRECISION NOT NULL,
    sigma_consumption  DOUBLE PRECISION NOT NULL,
    median_cumulative  DOUBLE PRECISION NOT NULL,
    sigma_cumulative   DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Правила для ГВС
CREATE TABLE IF NOT EXISTS rule_params_gvs (
    sensor_id TEXT PRIMARY KEY,
    median_supply DOUBLE PRECISION NOT NULL,
    sigma_supply  DOUBLE PRECISION NOT NULL,
    median_return DOUBLE PRECISION NOT NULL,
    sigma_return  DOUBLE PRECISION NOT NULL,
    median_dt     DOUBLE PRECISION NOT NULL,
    sigma_dt      DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now()
);
