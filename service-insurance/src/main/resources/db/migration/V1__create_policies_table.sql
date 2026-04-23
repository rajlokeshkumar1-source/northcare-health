-- V1: Create insurance_policies table
CREATE TABLE insurance_policies (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    policy_number       VARCHAR(20)     NOT NULL,
    patient_id          UUID            NOT NULL,
    provider_id         UUID            NOT NULL,
    provider_name       VARCHAR(150)    NOT NULL,
    policy_type         VARCHAR(20)     NOT NULL,
    coverage_start_date DATE            NOT NULL,
    coverage_end_date   DATE            NOT NULL,
    deductible_amount   NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    deductible_met      NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    coverage_limit      NUMERIC(12, 2),
    covered_amount      NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    group_number        VARCHAR(50),
    member_number       VARCHAR(50),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_insurance_policies PRIMARY KEY (id),
    CONSTRAINT uq_policy_number       UNIQUE (policy_number),
    CONSTRAINT chk_policy_type        CHECK (policy_type IN ('BASIC', 'EXTENDED', 'PREMIUM', 'GOVERNMENT')),
    CONSTRAINT chk_policy_status      CHECK (status IN ('ACTIVE', 'EXPIRED', 'SUSPENDED', 'CANCELLED')),
    CONSTRAINT chk_coverage_dates     CHECK (coverage_end_date >= coverage_start_date),
    CONSTRAINT chk_deductible_met     CHECK (deductible_met >= 0),
    CONSTRAINT chk_covered_amount     CHECK (covered_amount >= 0)
);

-- Indexes
CREATE INDEX idx_policies_patient_id ON insurance_policies (patient_id);
CREATE INDEX idx_policies_status     ON insurance_policies (status);
CREATE INDEX idx_policies_type       ON insurance_policies (policy_type);

COMMENT ON TABLE  insurance_policies                   IS 'Stores insurance policy records per patient';
COMMENT ON COLUMN insurance_policies.policy_number     IS 'Human-readable policy ID, e.g. POL-2025-00001';
COMMENT ON COLUMN insurance_policies.deductible_met    IS 'Amount already applied toward the deductible this period';
COMMENT ON COLUMN insurance_policies.covered_amount    IS 'Total amount paid by insurer to date under this policy';
