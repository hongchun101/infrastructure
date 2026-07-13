insert into roles (id, code, name) values
('00000000-0000-0000-0000-000000000102', 'BACKEND_OPERATOR', 'Backend operator');

insert into permissions (id, code, name) values
('00000000-0000-0000-0000-000000000206', 'backend:account:write', 'Manage backend accounts');

insert into role_permissions (role_id, permission_id) values
('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000206');

create table backend_accounts (
    id uuid primary key,
    username varchar(100) not null unique,
    email varchar(255) unique,
    phone varchar(32) unique,
    password_hash varchar(100) not null,
    display_name varchar(100) not null,
    enabled boolean not null,
    created_time timestamp not null,
    updated_time timestamp not null
);

create table backend_account_roles (
    account_id uuid not null references backend_accounts(id) on delete cascade,
    role_id uuid not null references roles(id),
    primary key (account_id, role_id)
);

insert into backend_accounts (id, username, email, phone, password_hash, display_name, enabled, created_time, updated_time) values
('00000000-0000-0000-0000-000000000301', 'operator', 'operator@example.com', '13800000002', '$2a$10$IbukqwN.OfTRNBkBDJHQgOUUIYxD9A8qeR67En4RRbmR5H1tjytCS', '\u540e\u53f0\u64cd\u4f5c\u5458', true, '2026-06-06 00:00:00', '2026-06-06 00:00:00');

insert into backend_account_roles (account_id, role_id) values
('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000102');
