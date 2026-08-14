create table task_execution (
    id uuid primary key,
    tenant_id text not null,
    task_type text not null,
    status text not null,
    description text,
    attempt_count integer not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    dispatched_at timestamptz,
    completed_at timestamptz,
    constraint ck_task_execution_type check (task_type in ('NO_OP')),
    constraint ck_task_execution_status check (status in ('PENDING', 'DISPATCHED', 'SUCCEEDED', 'FAILED'))
);

create index idx_task_execution_tenant_created
    on task_execution (tenant_id, created_at desc);

create index idx_task_execution_status_created
    on task_execution (status, created_at);
