-- V2: Create claims table
CREATE TABLE claims (
    id               UUID            NOT NULL DEFAULT gen_random_uuid(),
    claim_number     VARCHAR(20)     NOT NULL,
    policy_id        UUID            NOT NULL,
    patient_id       UUID            NOT NULL,
    invoice_id       UUID,
    claim_date       DATE            NOT NULL,
    service_date     DATE            NOT NULL,
    diagnosis_codes  JSONB,
    procedure_codes  JSONB,
    billed_amount    NUMERIC(12, 2)  NOT NULL,
    approved_amount  NUMERIC(12, 2),
    paid_amount      NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    status           VARCHAR(25)     NOT NULL DEFAULT 'SUBMITTED',
    denial_reason    TEXT,
    notes            TEXT,
    processed_at     TIMESTAMP,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_claims        PRIMARY KEY (id),
    CONSTRAINT uq_claim_number  UNIQUE (claim_number),
    CONSTRAINT fk_claim_policy  FOREIGN KEY (policy_id) REFERENCES insurance_policies (id),
    CONSTRAINT chk_claim_status CHECK (status IN ('SUBMITTED','UNDER_REVIEW','APPROVED','PARTIALLY_APPROVED','DENIED','APPEALED','PAID')),
    CONSTRAINT chk_billed_pos   CHECK (billed_amount > 0),
    CONSTRAINT chk_paid_pos     CHECK (paid_amount >= 0)
);

-- Indexes
CREATE INDEX idx_claims_policy_id  ON claims (policy_id);
CREATE INDEX idx_claims_patient_id ON claims (patient_id);
CREATE INDEX idx_claims_status     ON claims (status);
CREATE INDEX idx_claims_claim_date ON claims (claim_date);
CREATE INDEX idx_claims_invoice_id ON claims (invoice_id) WHERE invoice_id IS NOT NULL;

-- GIN index for JSON searches
CREATE INDEX idx_claims_diagnosis_gin   ON claims USING GIN (diagnosis_codes);
CREATE INDEX idx_claims_procedure_gin   ON claims USING GIN (procedure_codes);

COMMENT ON TABLE  claims              IS 'Insurance claim lifecycle records';
COMMENT ON COLUMN claims.claim_number IS 'Human-readable claim ID, e.g. CLM-2025-00001';
COMMENT ON COLUMN claims.invoice_id   IS 'Optional reference to a billing-service invoice';
