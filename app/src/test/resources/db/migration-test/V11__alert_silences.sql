CREATE TABLE alert_silences (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    rule_id      UUID         NULL,
    starts_at    timestamp  NOT NULL,
    ends_at      timestamp  NOT NULL,
    reason       VARCHAR(500) NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by   VARCHAR(255) NULL,
    created_at   timestamp  NOT NULL DEFAULT NOW(),
    updated_at   timestamp  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_alert_silences_window CHECK (ends_at > starts_at)
);

CREATE INDEX idx_alert_silences_rule_window
    ON alert_silences (rule_id, starts_at, ends_at);
