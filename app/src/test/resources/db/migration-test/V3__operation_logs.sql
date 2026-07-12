-- Operation log (test environment: H2 in PostgreSQL mode, no native partition support)

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
);

create index operation_logs_created_time_idx on operation_logs (created_time desc);
create index operation_logs_user_id_idx on operation_logs (user_id, created_time desc);
create index operation_logs_module_idx on operation_logs (module, created_time desc);
create index operation_logs_success_idx on operation_logs (success, created_time desc);

insert into permissions (id, code, name) values
    ('00000000-0000-0000-0000-000000000205', 'operation:log:read', 'Read operation logs');

insert into role_permissions (role_id, permission_id) values
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000205');
