-- 操作日志（按 created_time 月份分区）

create table operation_logs (
    id uuid not null,
    trace_id varchar(64),
    user_id uuid,
    username varchar(100),
    module varchar(50) not null,
    action varchar(100) not null,
    description varchar(500),
    method varchar(10) not null,
    path varchar(500) not null,
    query_string varchar(2000),
    response_status integer,
    error_message varchar(2000),
    client_ip varchar(64),
    user_agent varchar(500),
    duration_ms bigint not null,
    success boolean not null,
    created_time timestamp not null,
    primary key (id, created_time)
) partition by range (created_time);

create index operation_logs_created_time_idx on operation_logs (created_time desc);
create index operation_logs_user_id_idx on operation_logs (user_id, created_time desc);
create index operation_logs_module_idx on operation_logs (module, created_time desc);
create index operation_logs_success_idx on operation_logs (success, created_time desc);

do $$
declare
    current_month date := date_trunc('month', now())::date;
    next_month date := (date_trunc('month', now()) + interval '1 month')::date;
    partition_name text;
begin
    partition_name := 'operation_logs_' || to_char(current_month, 'YYYYMM');
    execute format(
        'create table %I partition of operation_logs for values from (%L) to (%L)',
        partition_name, current_month, next_month
    );
    partition_name := 'operation_logs_' || to_char(next_month, 'YYYYMM');
    execute format(
        'create table %I partition of operation_logs for values from (%L) to (%L)',
        partition_name, next_month, (next_month + interval '1 month')::date
    );
end $$;

insert into permissions (id, code, name) values
    ('00000000-0000-0000-0000-000000000205', 'operation:log:read', 'Read operation logs');

insert into role_permissions (role_id, permission_id) values
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000205');