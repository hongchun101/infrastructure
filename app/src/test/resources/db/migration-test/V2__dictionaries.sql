create table dictionary_categories (
    id uuid primary key,
    code varchar(100) not null unique,
    name varchar(100) not null,
    description varchar(500),
    enabled boolean not null,
    created_time timestamp not null
);

create table dictionary_items (
    id uuid primary key,
    category_id uuid not null references dictionary_categories(id) on delete cascade,
    code varchar(100) not null,
    name varchar(100) not null,
    parent_id uuid references dictionary_items(id) on delete restrict,
    sort_order integer not null default 0,
    enabled boolean not null,
    created_time timestamp not null,
    unique (category_id, code)
);

create index dictionary_items_category_parent_idx on dictionary_items (category_id, parent_id);