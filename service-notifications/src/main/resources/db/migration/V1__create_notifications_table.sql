CREATE TABLE notifications (
    id               UUID         PRIMARY KEY,
    recipient_id     UUID         NOT NULL,
    recipient_type   VARCHAR(20)  NOT NULL,
    recipient_email  VARCHAR(255),
    channel          VARCHAR(20)  NOT NULL,
    type             VARCHAR(50)  NOT NULL,
    subject          VARCHAR(255),
    message          TEXT         NOT NULL,
    priority         VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    scheduled_at     TIMESTAMP,
    sent_at          TIMESTAMP,
    failure_reason   TEXT,
    retry_count      INT          DEFAULT 0,
    max_retries      INT          DEFAULT 3,
    metadata         TEXT,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient  ON notifications(recipient_id);
CREATE INDEX idx_notifications_status     ON notifications(status);
CREATE INDEX idx_notifications_scheduled  ON notifications(scheduled_at) WHERE status = 'PENDING';
