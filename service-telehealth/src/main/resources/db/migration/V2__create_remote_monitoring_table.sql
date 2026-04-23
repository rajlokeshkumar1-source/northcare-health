-- V2: Create remote_monitoring_readings table
-- Device types: HEART_RATE_MONITOR, GLUCOMETER, BLOOD_PRESSURE, PULSE_OXIMETER, THERMOMETER
-- is_alert = TRUE when value is outside [alert_threshold_min, alert_threshold_max].

CREATE TABLE IF NOT EXISTS remote_monitoring_readings (
    id                  UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id          UUID             NOT NULL,
    device_id           VARCHAR(100)     NOT NULL,
    device_type         VARCHAR(30)      NOT NULL,
    metric_name         VARCHAR(100)     NOT NULL,
    value               DOUBLE PRECISION NOT NULL,
    unit                VARCHAR(50)      NOT NULL,
    is_alert            BOOLEAN          NOT NULL DEFAULT FALSE,
    alert_threshold_min DOUBLE PRECISION,
    alert_threshold_max DOUBLE PRECISION,
    recorded_at         TIMESTAMP        NOT NULL,
    created_at          TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_device_type
        CHECK (device_type IN (
            'HEART_RATE_MONITOR','GLUCOMETER','BLOOD_PRESSURE','PULSE_OXIMETER','THERMOMETER'
        ))
);

CREATE INDEX IF NOT EXISTS idx_monitoring_patient_id
    ON remote_monitoring_readings (patient_id);

CREATE INDEX IF NOT EXISTS idx_monitoring_recorded_at
    ON remote_monitoring_readings (recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_monitoring_patient_recorded
    ON remote_monitoring_readings (patient_id, recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_monitoring_patient_alert
    ON remote_monitoring_readings (patient_id, is_alert)
    WHERE is_alert = TRUE;
