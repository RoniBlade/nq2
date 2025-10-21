CREATE TABLE IF NOT EXISTS public.prediction_history (
    id BIGSERIAL PRIMARY KEY,
    sensor_id TEXT NOT NULL,
    ts TIMESTAMPTZ NOT NULL DEFAULT now(),
    source TEXT NOT NULL, -- "rule" или "ml"
    input_data JSONB NOT NULL,
    exclude_fields TEXT[] DEFAULT '{}',
    prediction JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);
