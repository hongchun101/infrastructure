CREATE TABLE export_jobs (
    id              BIGSERIAL     PRIMARY KEY,
    service         VARCHAR(64)   NOT NULL DEFAULT 'export',
    business_type   VARCHAR(64)   NOT NULL,
    format          VARCHAR(8)    NOT NULL DEFAULT 'xlsx',
    file_name       VARCHAR(255)  NOT NULL,
    params          JSONB         NULL,
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    total_rows      BIGINT        NULL,
    processed_rows  BIGINT        NOT NULL DEFAULT 0,
    file_id         BIGINT        NULL,
    error           TEXT          NULL,
    owner_user_id   UUID          NULL,
    started_at      TIMESTAMPTZ   NULL,
    finished_at     TIMESTAMPTZ   NULL,
    duration_ms     BIGINT        NULL,
    expires_at      TIMESTAMPTZ   NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ   NULL,
    CONSTRAINT ck_export_jobs_format CHECK (format IN ('xlsx', 'csv')),
    CONSTRAINT ck_export_jobs_status CHECK (status IN ('PENDING','RUNNING','SUCCESS','FAILED','CANCELLED'))
);

CREATE INDEX idx_export_jobs_owner_created   ON export_jobs (owner_user_id, created_at DESC);
CREATE INDEX idx_export_jobs_status_created  ON export_jobs (status, created_at);
CREATE INDEX idx_export_jobs_active          ON export_jobs (status) WHERE deleted_at IS NULL;