-- Permissions for the scheduler module
INSERT INTO permissions (id, code, name) VALUES
    ('00000000-0000-0000-0000-00000000020c', 'job:read', 'Read job definitions and executions'),
    ('00000000-0000-0000-0000-00000000020d', 'job:write', 'Pause/resume/trigger jobs');

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-00000000020c'),
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-00000000020d');