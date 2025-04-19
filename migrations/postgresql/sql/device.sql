CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS device (
    device_id           TEXT                PRIMARY KEY,
    model_id            TEXT                NOT NULL,
    environment_temp    DOUBLE PRECISION    NOT NULL,
    usage_hours         DOUBLE PRECISION    NOT NULL,
    install_date        TIMESTAMPTZ         NOT NULL,
    secret              BYTEA               NOT NULL
);
