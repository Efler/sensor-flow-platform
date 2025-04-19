CREATE TABLE IF NOT EXISTS model_params (
    model_id            TEXT                                                NOT NULL,
    metric_name         TEXT                                                NOT NULL,
    threshold_type      TEXT CHECK (threshold_type IN ('upper', 'lower'))   NOT NULL,
    a0                  DOUBLE PRECISION                                    NOT NULL,
    a1                  DOUBLE PRECISION                                    NOT NULL,
    a2                  DOUBLE PRECISION                                    NOT NULL,
    a3                  DOUBLE PRECISION                                    NOT NULL,
    notes               TEXT,

    PRIMARY KEY (model_id, metric_name, threshold_type)
);
