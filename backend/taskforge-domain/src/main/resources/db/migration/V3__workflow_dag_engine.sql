alter table task_execution
    drop constraint ck_task_execution_status;

alter table task_execution
    add constraint ck_task_execution_status
        check (status in ('BLOCKED', 'PENDING', 'DISPATCHED', 'SUCCEEDED', 'FAILED'));

create table workflow (
    id uuid primary key,
    name text not null,
    description text,
    status text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint ck_workflow_status check (status in ('ACTIVE', 'ARCHIVED'))
);

create table workflow_version (
    id uuid primary key,
    workflow_id uuid not null references workflow(id) on delete cascade,
    version_number integer not null,
    status text not null,
    created_at timestamptz not null,
    published_at timestamptz,
    constraint ck_workflow_version_status check (status in ('DRAFT', 'PUBLISHED')),
    constraint uq_workflow_version_number unique (workflow_id, version_number)
);

create table workflow_node (
    id uuid primary key,
    workflow_version_id uuid not null references workflow_version(id) on delete cascade,
    node_key text not null,
    type text not null,
    name text not null,
    configuration text not null,
    created_at timestamptz not null,
    constraint ck_workflow_node_type check (type in ('NO_OP')),
    constraint uq_workflow_node_key unique (workflow_version_id, node_key)
);

create table workflow_edge (
    id uuid primary key,
    workflow_version_id uuid not null references workflow_version(id) on delete cascade,
    source_node_key text not null,
    target_node_key text not null,
    constraint ck_workflow_edge_no_self check (source_node_key <> target_node_key),
    constraint uq_workflow_edge unique (workflow_version_id, source_node_key, target_node_key),
    constraint fk_workflow_edge_source foreign key (workflow_version_id, source_node_key)
        references workflow_node(workflow_version_id, node_key) on delete cascade,
    constraint fk_workflow_edge_target foreign key (workflow_version_id, target_node_key)
        references workflow_node(workflow_version_id, node_key) on delete cascade
);

create table workflow_run (
    id uuid primary key,
    workflow_id uuid not null references workflow(id),
    workflow_version_id uuid not null references workflow_version(id),
    status text not null,
    created_at timestamptz not null,
    started_at timestamptz not null,
    completed_at timestamptz,
    failure_message text,
    constraint ck_workflow_run_status check (status in ('RUNNING', 'SUCCEEDED', 'FAILED'))
);

alter table task_execution
    add column workflow_run_id uuid references workflow_run(id),
    add column workflow_node_key text,
    add constraint uq_task_execution_workflow_node unique (workflow_run_id, workflow_node_key);

create index idx_workflow_version_workflow_status
    on workflow_version (workflow_id, status, version_number desc);

create index idx_workflow_node_version
    on workflow_node (workflow_version_id);

create index idx_workflow_edge_version_target
    on workflow_edge (workflow_version_id, target_node_key);

create index idx_workflow_run_status
    on workflow_run (status, created_at);

create index idx_task_execution_workflow_run
    on task_execution (workflow_run_id, status);
