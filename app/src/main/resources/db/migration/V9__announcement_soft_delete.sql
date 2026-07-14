-- Soft-delete support for announcements

alter table announcements
    add column deleted_at timestamp;

create index announcements_active_idx
    on announcements (created_time desc)
    where deleted_at is null;
