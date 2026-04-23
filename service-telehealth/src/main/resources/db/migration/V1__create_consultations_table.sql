-- V1: Create consultations table
-- Status values: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
-- Type values:   VIDEO, AUDIO, CHAT
-- NOTE: notes column contains PHI (HIPAA) – restrict access at application layer.

CREATE TABLE IF NOT EXISTS consultations (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id        UUID         NOT NULL,
    doctor_id         UUID         NOT NULL,
    doctor_name       VARCHAR(255) NOT NULL,
    scheduled_at      TIMESTAMP    NOT NULL,
    actual_start_at   TIMESTAMP,
    actual_end_at     TIMESTAMP,
    status            VARCHAR(20)  NOT NULL,
    consultation_type VARCHAR(10)  NOT NULL,
    chief_complaint   TEXT,
    notes             TEXT,                        -- PHI: HIPAA protected doctor notes
    meeting_url       VARCHAR(500),
    duration_minutes  INTEGER,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_consultation_status
        CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW')),
    CONSTRAINT chk_consultation_type
        CHECK (consultation_type IN ('VIDEO','AUDIO','CHAT'))
);

CREATE INDEX IF NOT EXISTS idx_consultation_patient_id
    ON consultations (patient_id);

CREATE INDEX IF NOT EXISTS idx_consultation_status
    ON consultations (status);

CREATE INDEX IF NOT EXISTS idx_consultation_scheduled_at
    ON consultations (scheduled_at);

CREATE INDEX IF NOT EXISTS idx_consultation_patient_status
    ON consultations (patient_id, status);

CREATE INDEX IF NOT EXISTS idx_consultation_active_scheduled
    ON consultations (is_active, scheduled_at)
    WHERE is_active = TRUE;
