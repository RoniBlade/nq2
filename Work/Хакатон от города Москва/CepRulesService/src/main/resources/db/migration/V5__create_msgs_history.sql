-- Drop table
-- DROP TABLE public.hvs_messages;

CREATE TABLE IF NOT EXISTS public.hvs_messages (
    id BIGSERIAL PRIMARY KEY,
    sensor_id TEXT NOT NULL,
    asset_id  TEXT,
    ts        TIMESTAMPTZ NOT NULL,
    date_str  TEXT,
    time_str  TEXT,
    cumulative_consumption DOUBLE PRECISION,
    consumption_for_period DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT now()
);

   -- Drop table
-- DROP TABLE public.gvs_messages;

CREATE TABLE IF NOT EXISTS public.gvs_messages (
    id BIGSERIAL PRIMARY KEY,
    sensor_id TEXT NOT NULL,
    asset_id  TEXT,
    ts        TIMESTAMPTZ NOT NULL,
    date_str  TEXT,
    time_str  TEXT,
    supply    DOUBLE PRECISION,
    return_val DOUBLE PRECISION,
    consumption_for_period DOUBLE PRECISION,
    t1 DOUBLE PRECISION,
    t2 DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT now()
);
