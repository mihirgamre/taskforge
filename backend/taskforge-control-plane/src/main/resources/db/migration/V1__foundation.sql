create table if not exists schema_history_marker (
    id uuid primary key,
    description text not null,
    created_at timestamptz not null default now()
);

