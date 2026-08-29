create table outbox_event (
    id uuid primary key,
    aggregate_type text not null,
    aggregate_id uuid not null,
    event_type text not null,
    topic text not null,
    event_key text not null,
    payload text not null,
    status text not null,
    attempt_count integer not null default 0,
    next_attempt_at timestamptz not null,
    created_at timestamptz not null,
    published_at timestamptz,
    last_error text,
    constraint ck_outbox_event_status check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
);

create table inbox_event (
    id uuid not null,
    event_type text not null,
    consumer_name text not null,
    processed_at timestamptz not null,
    primary key (id, consumer_name)
);

create table dead_letter_task (
    id uuid primary key,
    task_id uuid not null references task_execution(id),
    reason text not null,
    created_at timestamptz not null,
    constraint uq_dead_letter_task unique (task_id)
);

alter table task_execution
    add column lease_owner text,
    add column lease_token uuid,
    add column lease_expires_at timestamptz,
    add column lease_heartbeat_at timestamptz,
    add column next_attempt_at timestamptz,
    add column max_attempts integer not null default 3,
    add column failure_message text;

update task_execution
set next_attempt_at = created_at
where next_attempt_at is null;

alter table task_execution
    alter column next_attempt_at set not null,
    add constraint ck_task_execution_attempts check (attempt_count >= 0 and max_attempts >= 1);

create index idx_outbox_event_ready
    on outbox_event (status, next_attempt_at, created_at);

create index idx_outbox_event_aggregate
    on outbox_event (aggregate_type, aggregate_id);

create index idx_task_execution_ready
    on task_execution (status, next_attempt_at, created_at);

create index idx_task_execution_lease_expiry
    on task_execution (status, lease_expires_at)
    where status = 'DISPATCHED';

create index idx_dead_letter_task_created
    on dead_letter_task (created_at);
