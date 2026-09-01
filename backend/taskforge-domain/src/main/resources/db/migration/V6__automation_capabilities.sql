alter table workflow_node
    drop constraint ck_workflow_node_type;

alter table workflow_node
    add constraint ck_workflow_node_type
        check (type in ('NO_OP', 'HTTP', 'TRANSFORM', 'APPROVAL', 'NOTIFICATION'));

alter table task_execution
    drop constraint ck_task_execution_type;

alter table task_execution
    add constraint ck_task_execution_type
        check (task_type in ('NO_OP', 'HTTP', 'TRANSFORM', 'APPROVAL', 'NOTIFICATION'));

alter table task_execution
    drop constraint ck_task_execution_status;

alter table task_execution
    add constraint ck_task_execution_status
        check (status in ('BLOCKED', 'PENDING', 'DISPATCHED', 'WAITING_APPROVAL', 'SUCCEEDED', 'FAILED'));

alter table task_execution
    add column task_configuration text not null default '{}',
    add column task_result text;

create table api_key (
    id uuid primary key,
    organization_id uuid not null references organization_account(id) on delete cascade,
    name text not null,
    key_prefix text not null,
    key_hash text not null,
    created_at timestamptz not null,
    expires_at timestamptz,
    revoked_at timestamptz,
    constraint uq_api_key_hash unique (key_hash)
);

create index idx_api_key_organization
    on api_key (organization_id, created_at desc);

create index idx_api_key_prefix
    on api_key (key_prefix);

create table workflow_schedule (
    id uuid primary key,
    organization_id uuid not null references organization_account(id) on delete cascade,
    workflow_id uuid not null references workflow(id) on delete cascade,
    name text not null,
    cron_expression text not null,
    time_zone text not null,
    enabled boolean not null,
    next_run_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_run_at timestamptz
);

create index idx_workflow_schedule_due
    on workflow_schedule (enabled, next_run_at);

create index idx_workflow_schedule_organization
    on workflow_schedule (organization_id, created_at desc);
