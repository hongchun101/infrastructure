-- Alert module schema

create table alert_rules (
    id uuid primary key,
    code varchar(64) not null unique,
    name varchar(200) not null,
    description varchar(500),
    rule_type varchar(32) not null,
    severity varchar(16) not null,
    enabled boolean not null default true,
    source_module varchar(64),
    source_action varchar(100),
    config jsonb not null default '{}'::jsonb,
    channels jsonb not null default '[]'::jsonb,
    created_time timestamp not null,
    updated_time timestamp not null
);

create index alert_rules_enabled_idx on alert_rules (enabled);

create table alert_events (
    id uuid primary key,
    rule_id uuid not null references alert_rules(id) on delete cascade,
    fingerprint varchar(128) not null unique,
    source_module varchar(64),
    source_action varchar(100),
    severity varchar(16) not null,
    summary varchar(500) not null,
    detail jsonb,
    first_seen_at timestamp not null,
    last_seen_at timestamp not null,
    occurrences bigint not null default 1,
    resolved boolean not null default false,
    resolved_at timestamp,
    created_time timestamp not null,
    updated_time timestamp not null
);

create index alert_events_rule_idx on alert_events (rule_id, last_seen_at desc);
create index alert_events_unresolved_idx on alert_events (resolved, last_seen_at desc);

create table alert_notifications (
    id uuid primary key,
    event_id uuid not null references alert_events(id) on delete cascade,
    channel varchar(32) not null,
    target varchar(500) not null,
    status varchar(16) not null,
    http_status integer,
    error_message varchar(2000),
    payload jsonb,
    sent_at timestamp not null,
    created_time timestamp not null
);

create index alert_notifications_event_idx on alert_notifications (event_id, sent_at desc);

-- Permissions for the alert module: read alerts and manage rules
insert into permissions (id, code, name) values
    ('00000000-0000-0000-0000-000000000207', 'alert:rule:read', 'Read alert rules'),
    ('00000000-0000-0000-0000-000000000208', 'alert:rule:write', 'Manage alert rules'),
    ('00000000-0000-0000-0000-000000000209', 'alert:event:read', 'Read alert events'),
    ('00000000-0000-0000-0000-00000000020a', 'alert:event:write', 'Resolve alert events');

-- Grant the admin role full alert access by default
insert into role_permissions (role_id, permission_id) values
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000207'),
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000208'),
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000209'),
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-00000000020a');

-- Backoffice operator: read-only on alerts
insert into role_permissions (role_id, permission_id) values
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000207'),
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000209');
