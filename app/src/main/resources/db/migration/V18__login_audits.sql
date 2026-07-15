-- Login audit (登录审计：记录登录成功/失败/登出事件，供安全合规与异常登录排查使用)

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
) partition by range (created_time);

create index login_audits_created_time_idx on login_audits (created_time desc);
create index login_audits_principal_idx on login_audits (principal, created_time desc);
create index login_audits_outcome_idx on login_audits (outcome, created_time desc);
create index login_audits_account_id_idx on login_audits (account_id, created_time desc);

do $$
declare
    current_month date := date_trunc('month', now())::date;
    next_month date := (date_trunc('month', now()) + interval '1 month')::date;
    partition_name text;
begin
    partition_name := 'login_audits_' || to_char(current_month, 'YYYYMM');
    execute format(
        'create table %I partition of login_audits for values from (%L) to (%L)',
        partition_name, current_month, next_month
    );
    partition_name := 'login_audits_' || to_char(next_month, 'YYYYMM');
    execute format(
        'create table %I partition of login_audits for values from (%L) to (%L)',
        partition_name, next_month, (next_month + interval '1 month')::date
    );
end $$;

insert into permissions (id, code, name) values
    ('00000000-0000-0000-0000-000000000220', 'login:audit:read', 'Read login audits');
insert into role_permissions (role_id, permission_id) values
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000220');
