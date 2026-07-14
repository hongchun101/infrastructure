CREATE TABLE file_objects (
    id                BIGSERIAL     PRIMARY KEY,
    biz_type          VARCHAR(64)   NOT NULL,
    biz_id            VARCHAR(64)   NULL,
    bucket            VARCHAR(64)   NOT NULL,
    object_key        VARCHAR(512)  NOT NULL,
    original_name     VARCHAR(255)  NOT NULL,
    content_type      VARCHAR(127)  NOT NULL,
    size_bytes        BIGINT        NULL,
    sha256            VARCHAR(64)   NULL,
    storage_provider  VARCHAR(32)   NOT NULL,
    visibility        VARCHAR(16)   NOT NULL DEFAULT 'PRIVATE',
    status            VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    owner_user_id     UUID          NULL,
    metadata          JSONB         NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    uploaded_at       TIMESTAMPTZ   NULL,
    expires_at        TIMESTAMPTZ   NULL,
    deleted_at        TIMESTAMPTZ   NULL,
    CONSTRAINT uk_file_objects_bucket_key UNIQUE (bucket, object_key)
);

CREATE INDEX idx_file_objects_biz ON file_objects (biz_type, biz_id);
CREATE INDEX idx_file_objects_owner_created ON file_objects (owner_user_id, created_at DESC);
CREATE INDEX idx_file_objects_pending ON file_objects (status) WHERE status = 'PENDING';
CREATE INDEX idx_file_objects_expires ON file_objects (expires_at) WHERE expires_at IS NOT NULL;