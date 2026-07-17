CREATE TABLE user_notifications (
    id                BIGSERIAL     PRIMARY KEY,
    recipient_user_id UUID          NOT NULL,
    category          VARCHAR(64)   NOT NULL,
    title             VARCHAR(255)  NOT NULL,
    content           TEXT          NOT NULL,
    link_url          VARCHAR(500)  NULL,
    payload           JSONB         NULL,
    priority          SMALLINT      NOT NULL DEFAULT 0,
    read_at           timestamp   NULL,
    archived_at       timestamp   NULL,
    created_at        timestamp   NOT NULL DEFAULT NOW(),
    expires_at        timestamp   NULL,
    CONSTRAINT ck_user_notifications_priority CHECK (priority BETWEEN 0 AND 2)
);

CREATE INDEX idx_user_notifications_recipient_created
    ON user_notifications (recipient_user_id, created_at DESC);

CREATE INDEX idx_user_notifications_recipient_unread
    ON user_notifications (recipient_user_id);

CREATE INDEX idx_user_notifications_expires
    ON user_notifications (expires_at);

-- Permissions for the notification module
INSERT INTO permissions (id, code, name) VALUES
    ('00000000-0000-0000-0000-00000000020b', 'notification:write', 'Send notifications to users');

-- Grant the admin role permission to send notifications
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-00000000020b');
