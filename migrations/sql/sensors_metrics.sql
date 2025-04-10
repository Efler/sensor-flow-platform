CREATE TABLE IF NOT EXISTS sensors_metrics (
    device_id       TEXT                NOT NULL,
    metric_name     TEXT                NOT NULL,
    metric_value    DOUBLE PRECISION    NOT NULL,
    src_timestamp   TIMESTAMPTZ         NOT NULL,

    CONSTRAINT sensors_metrics_pk PRIMARY KEY (device_id, metric_name, src_timestamp)
);

SELECT create_hypertable('sensors_metrics', 'src_timestamp', chunk_time_interval => INTERVAL '1 hour');

CREATE INDEX sensors_metrics_idx_device_time ON sensors_metrics (device_id, src_timestamp);
