-- Extend soft-delete to dictionary, project, backend_account

alter table dictionary_categories
    add column deleted_at timestamp;

create index dictionary_categories_active_idx
    on dictionary_categories (code)
   ;

alter table dictionary_items
    add column deleted_at timestamp;

create index dictionary_items_active_idx
    on dictionary_items (category_id, code)
   ;

alter table projects
    add column deleted_at timestamp;

create index projects_active_idx
    on projects (owner_id, created_time desc)
   ;

alter table backend_accounts
    add column deleted_at timestamp;

create index backend_accounts_active_idx
    on backend_accounts (username)
   ;
