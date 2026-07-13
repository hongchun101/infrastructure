alter table announcements
    add column publish_at timestamp;

create index announcements_publish_at_idx on announcements (publish_at);

create table announcement_reads (
    id uuid primary key,
    announcement_id uuid not null references announcements(id) on delete cascade,
    user_id uuid not null references users(id),
    read_at timestamp not null,
    constraint announcement_reads_announcement_user_uk unique (announcement_id, user_id)
);

create index announcement_reads_user_idx on announcement_reads (user_id);
create index announcement_reads_announcement_idx on announcement_reads (announcement_id);
