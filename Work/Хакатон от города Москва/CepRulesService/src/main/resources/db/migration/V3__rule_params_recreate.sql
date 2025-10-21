-- V2__rule_params_recreate.sql

DROP TABLE IF EXISTS rule_params_hvs CASCADE;
DROP TABLE IF EXISTS rule_params_gvs CASCADE;

CREATE TABLE public.rule_params_hvs (
    sensor_id text NOT NULL,
    hour_of_day int2 NOT NULL,
    is_weekend bool NOT NULL,
    median_consumption float8 NULL,
    sigma_consumption float8 NULL,
    upper_consumption float8 NULL,
    median_cumulative float8 NULL,
    sigma_cumulative float8 NULL,
    updated_at timestamptz DEFAULT now() NULL,
    CONSTRAINT rule_params_hvs_pkey PRIMARY KEY (sensor_id, hour_of_day, is_weekend)
);

CREATE TABLE public.rule_params_gvs (
    sensor_id text NOT NULL,
    hour_of_day int2 NOT NULL,
    is_weekend bool NOT NULL,
    median_supply float8 NULL,
    sigma_supply float8 NULL,
    median_return float8 NULL,
    sigma_return float8 NULL,
    median_dt float8 NULL,
    sigma_dt float8 NULL,
    updated_at timestamptz DEFAULT now() NULL,
    CONSTRAINT rule_params_gvs_pkey PRIMARY KEY (sensor_id, hour_of_day, is_weekend)
);
