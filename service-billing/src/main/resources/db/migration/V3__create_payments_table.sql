CREATE TABLE payments
(
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    invoice_id       UUID           NOT NULL,
    amount           NUMERIC(12, 2) NOT NULL,
    payment_date     DATE           NOT NULL,
    payment_method   VARCHAR(20)    NOT NULL,
    reference_number VARCHAR(100),
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    processed_at     TIMESTAMP,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payments       PRIMARY KEY (id),
    CONSTRAINT fk_pay_invoice    FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT chk_pay_amount    CHECK (amount > 0),
    CONSTRAINT chk_pay_method    CHECK (payment_method IN (
        'CREDIT_CARD', 'BANK_TRANSFER', 'INSURANCE', 'CASH', 'CHEQUE'
    )),
    CONSTRAINT chk_pay_status    CHECK (status IN (
        'PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'
    ))
);

CREATE INDEX idx_payments_invoice_id   ON payments (invoice_id);
CREATE INDEX idx_payments_payment_date ON payments (payment_date);
CREATE INDEX idx_payments_status       ON payments (status);
