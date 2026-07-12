create table users (
    id uuid primary key,
    username varchar(100) not null unique,
    email varchar(255) unique,
    phone varchar(32) unique,
    password_hash varchar(100) not null,
    display_name varchar(100) not null,
    enabled boolean not null,
    created_time timestamp not null
);

create table roles (
    id uuid primary key,
    code varchar(100) not null unique,
    name varchar(100) not null
);

create table permissions (
    id uuid primary key,
    code varchar(100) not null unique,
    name varchar(100) not null
);

create table user_roles (
    user_id uuid not null references users(id),
    role_id uuid not null references roles(id),
    primary key (user_id, role_id)
);

create table role_permissions (
    role_id uuid not null references roles(id),
    permission_id uuid not null references permissions(id),
    primary key (role_id, permission_id)
);

create table projects (
    id uuid primary key,
    name varchar(200) not null,
    owner_id uuid not null references users(id),
    created_time timestamp not null
);

insert into users (id, username, email, phone, password_hash, display_name, enabled, created_time) values
('00000000-0000-0000-0000-000000000001', 'admin', 'admin@example.com', '13800000000', '$2a$10$IbukqwN.OfTRNBkBDJHQgOUUIYxD9A8qeR67En4RRbmR5H1tjytCS', 'Administrator', true, '2026-06-06 00:00:00'),
('00000000-0000-0000-0000-000000000002', 'disabled', 'disabled@example.com', '13800000001', '$2a$10$IbukqwN.OfTRNBkBDJHQgOUUIYxD9A8qeR67En4RRbmR5H1tjytCS', 'Disabled User', false, '2026-06-06 00:00:00');

insert into roles (id, code, name) values
('00000000-0000-0000-0000-000000000101', 'ADMIN', 'Administrator');

insert into permissions (id, code, name) values
('00000000-0000-0000-0000-000000000201', 'project:read', 'Read projects'),
('00000000-0000-0000-0000-000000000202', 'project:write', 'Write projects');

insert into user_roles (user_id, role_id) values
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000101'),
('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000101');

insert into role_permissions (role_id, permission_id) values
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000201'),
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000202');
