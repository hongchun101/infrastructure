create table announcements (
    id uuid primary key,
    title varchar(200) not null,
    summary varchar(500),
    content text not null,
    status varchar(16) not null,
    priority integer not null default 0,
    published_at timestamp,
    created_by uuid not null references users(id),
    updated_by uuid not null references users(id),
    created_time timestamp not null,
    updated_time timestamp not null,
    constraint announcements_status_check
        check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    constraint announcements_priority_check
        check (priority between 0 and 9)
);

create index announcements_status_idx on announcements (status);
create index announcements_published_at_idx on announcements (published_at desc);
create index announcements_listing_idx
    on announcements (status, priority desc, published_at desc);

insert into permissions (id, code, name) values
('00000000-0000-0000-0000-000000000203', 'announcement:read', 'Read announcements'),
('00000000-0000-0000-0000-000000000204', 'announcement:write', 'Write announcements');

insert into role_permissions (role_id, permission_id) values
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000203'),
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000204');
