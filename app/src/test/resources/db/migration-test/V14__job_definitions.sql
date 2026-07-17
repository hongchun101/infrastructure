CREATE TABLE job_definitions (
    id                            BIGSERIAL     PRIMARY KEY,
    code                          VARCHAR(128)  NOT NULL UNIQUE,
    name                          VARCHAR(255)  NOT NULL,
    description                   VARCHAR(500)  NULL,
    cron                          VARCHAR(64)   NULL,
    fixed_delay_seconds           INT           NULL,
    enabled                       BOOLEAN       NOT NULL DEFAULT TRUE,
    retry_max_attempts            INT           NOT NULL DEFAULT 3,
    retry_initial_backoff_seconds BIGINT        NOT NULL DEFAULT 5,
    retry_max_backoff_seconds     BIGINT        NOT NULL DEFAULT 300,
    retry_multiplier              DOUBLE PRECISION NOT NULL DEFAULT 2.0,
    timeout_seconds               INT           NOT NULL DEFAULT 300,
    payload                       JSONB         NULL,
    last_finished_at              timestamp   NULL,
    last_run_at                   timestamp   NULL,
    next_run_at                   timestamp   NULL,
    created_at                    timestamp   NOT NULL DEFAULT NOW(),
    updated_at                    timestamp   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_job_def_schedule CHECK (cron IS NOT NULL OR fixed_delay_seconds IS NOT NULL),
    CONSTRAINT ck_job_def_delay_positive CHECK (fixed_delay_seconds IS NULL OR fixed_delay_seconds > 0)
);

CREATE TABLE job_executions (
    id            BIGSERIAL     PRIMARY KEY,
    job_id        BIGINT        NOT NULL,
    status        VARCHAR(16)   NOT NULL,
    attempt       INT           NOT NULL DEFAULT 1,
    trigger_type  VARCHAR(16)   NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at  timestamp   NOT NULL,
    started_at    timestamp   NULL,
    finished_at   timestamp   NULL,
    duration_ms   BIGINT        NULL,
    result        VARCHAR(500)  NULL,
    error         TEXT          NULL,
    worker_id     VARCHAR(128)  NULL,
    next_run_at   timestamp   NULL,
    created_at    timestamp   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_job_executions_job FOREIGN KEY (job_id) REFERENCES job_definitions(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_executions_job_started ON job_executions (job_id, started_at DESC);
CREATE INDEX idx_job_executions_running     ON job_executions (status);
CREATE INDEX idx_job_executions_pending     ON job_executions (status);

CREATE INDEX idx_job_definitions_enabled_next
    ON job_definitions (enabled, next_run_at);
