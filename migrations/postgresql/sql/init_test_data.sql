CREATE OR REPLACE FUNCTION get_secrets_master_key()
RETURNS TEXT AS $$
BEGIN
    RETURN 'SECRETS-MASTER-KEY';
END;
$$ LANGUAGE plpgsql;

INSERT INTO device (device_id, model_id, environment_temp, usage_hours, install_date, secret) VALUES
('device-001', 'model-A', 25.0, 1500.0, '2022-01-01T00:00:00Z', pgp_sym_encrypt('secret-001', get_secrets_master_key())),
('device-002', 'model-A', 30.5, 3000.0, '2023-06-01T00:00:00Z', pgp_sym_encrypt('secret-002', get_secrets_master_key())),
('device-003', 'model-B', 20.0, 500.0,  '2021-05-15T00:00:00Z', pgp_sym_encrypt('secret-003', get_secrets_master_key()));


INSERT INTO model_params (model_id, metric_name, threshold_type, a0, a1, a2, a3, notes) VALUES
-- upper thresholds
('model-A', 'voltage', 'upper',  100.0, 0.5, 2.0, 0.05, 'Upper limit for voltage'),
('model-B', 'current', 'upper',   50.0, 0.2, 1.5, 0.03, 'Upper current safety'),
-- lower thresholds
('model-A', 'voltage', 'lower',   80.0, 0.3, 1.0, 0.02, 'Lower voltage boundary'),
('model-B', 'current', 'lower',   30.0, 0.1, 0.8, 0.01, 'Lower current cut-off');
