CREATE TABLE notification_outbox (
    id bigserial PRIMARY KEY,
    dedupe_key varchar(255) NOT NULL UNIQUE,
    recipient_email varchar(200) NOT NULL,
    subject varchar(255) NOT NULL,
    body text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED','SENT','FAILED')),
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at timestamptz NOT NULL DEFAULT now(),
    sent_at timestamptz,
    last_error varchar(120),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_outbox_ready ON notification_outbox(status, available_at, id);
