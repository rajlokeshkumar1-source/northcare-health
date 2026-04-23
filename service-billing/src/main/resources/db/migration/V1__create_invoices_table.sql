-- Invoice number sequence — each call to nextval gives a monotonically increasing integer
CREATE SEQUENCE IF NOT EXISTS invoice_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;

CREATE TABLE invoices
(
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(20)  NOT NULL,
    patient_id     UUID         NOT NULL,
    patient_name   VARCHAR(255) NOT NULL,
    service_date   DATE         NOT NULL,
    due_date       DATE         NOT NULL,
    subtotal       NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount     NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount   NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    currency       VARCHAR(3)   NOT NULL DEFAULT 'CAD',
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    notes          TEXT,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT uq_invoice_number UNIQUE (invoice_number),
    CONSTRAINT chk_invoice_status CHECK (status IN (
        'DRAFT', 'ISSUED', 'PAID', 'PARTIALLY_PAID', 'OVERDUE', 'CANCELLED', 'WRITTEN_OFF'
    )),
    CONSTRAINT chk_invoice_amounts CHECK (
        subtotal >= 0 AND tax_amount >= 0 AND total_amount >= 0
    )
);

CREATE INDEX idx_invoices_patient_id ON invoices (patient_id);
CREATE INDEX idx_invoices_status     ON invoices (status);
CREATE INDEX idx_invoices_due_date   ON invoices (due_date);
CREATE INDEX idx_invoices_active     ON invoices (is_active);
