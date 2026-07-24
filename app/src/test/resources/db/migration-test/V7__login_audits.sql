-- Login audit (test environment: H2 in PostgreSQL mode, no native partition support)

create table login_audits (
    id uuid not null,
    account_type varchar(16) not null,
    login_mode varchar(16),
    principal varchar(255),
    account_id uuid,
    username varchar(100),
    outcome varchar(16) not null,
    failure_reason varchar(500),
    client_ip varchar(64),
    user_agent varchar(500),
    trace_id varchar(64),
    created_time timestamp not null,
    primary key (id, created_time)
);

create index login_audits_created_time_idx on login_audits (created_time desc);
create index login_audits_principal_idx on login_audits (principal, created_time desc);
create index login_audits_outcome_idx on login_audits (outcome, created_time desc);
create index login_audits_account_id_idx on login_audits (account_id, created_time desc);

insert into permissions (id, code, name) values
    ('00000000-0000-0000-0000-000000000220', 'login:audit:read', 'Read login audits');
insert into role_permissions (role_id, permission_id) values
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000220');
