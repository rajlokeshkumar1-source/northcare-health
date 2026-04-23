CREATE TABLE invoice_line_items
(
    id           UUID           NOT NULL DEFAULT gen_random_uuid(),
    invoice_id   UUID           NOT NULL,
    service_code VARCHAR(20)    NOT NULL,
    description  VARCHAR(500)   NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    line_total   NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_line_items  PRIMARY KEY (id),
    CONSTRAINT fk_li_invoice  FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT chk_li_qty     CHECK (quantity > 0),
    CONSTRAINT chk_li_price   CHECK (unit_price >= 0),
    CONSTRAINT chk_li_total   CHECK (line_total >= 0)
);

CREATE INDEX idx_line_items_invoice_id ON invoice_line_items (invoice_id);
