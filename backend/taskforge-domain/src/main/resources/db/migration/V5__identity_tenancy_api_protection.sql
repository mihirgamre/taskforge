create table app_user (
    id uuid primary key,
    email text not null,
    password_hash text not null,
    status text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_app_user_email unique (email),
    constraint ck_app_user_status check (status in ('ACTIVE', 'DISABLED'))
);

create table organization_account (
    id uuid primary key,
    name text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table organization_membership (
    id uuid primary key,
    organization_id uuid not null references organization_account(id) on delete cascade,
    user_id uuid not null references app_user(id) on delete cascade,
    role text not null,
    created_at timestamptz not null,
    constraint uq_organization_membership unique (organization_id, user_id),
    constraint ck_organization_membership_role check (role in ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

create table refresh_token (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    organization_id uuid not null references organization_account(id) on delete cascade,
    token_hash text not null,
    family_id uuid not null,
    expires_at timestamptz not null,
    created_at timestamptz not null,
    revoked_at timestamptz,
    replaced_by_token_id uuid,
    constraint uq_refresh_token_hash unique (token_hash)
);

alter table workflow
    add column organization_id uuid references organization_account(id);

alter table workflow_run
    add column organization_id uuid references organization_account(id);

alter table task_execution
    add column organization_id uuid references organization_account(id);

create index idx_app_user_email
    on app_user (email);

create index idx_organization_membership_user
    on organization_membership (user_id, organization_id);

create index idx_refresh_token_hash
    on refresh_token (token_hash);

create index idx_refresh_token_user_family
    on refresh_token (user_id, family_id);

create index idx_workflow_organization
    on workflow (organization_id, created_at desc);

create index idx_workflow_run_organization
    on workflow_run (organization_id, created_at desc);

create index idx_task_execution_organization_created
    on task_execution (organization_id, created_at desc);
