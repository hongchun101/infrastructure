-- Alert window counters for failure-rate rules

create table alert_window_counters (
    id uuid primary key,
    rule_id uuid not null references alert_rules(id) on delete cascade,
    bucket_minute timestamp not null,
    total_count bigint not null,
    failed_count bigint not null,
    created_time timestamp not null,
    updated_time timestamp not null
);

create unique index alert_window_counters_rule_bucket_uidx
    on alert_window_counters (rule_id, bucket_minute);

create index alert_window_counters_bucket_idx
    on alert_window_counters (bucket_minute desc);
