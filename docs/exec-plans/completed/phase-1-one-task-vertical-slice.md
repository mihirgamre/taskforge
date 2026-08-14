# Phase 1 - One-Task Vertical Execution Slice

## Objective

Prove one minimal end-to-end execution path across the existing control-plane, scheduler, and worker boundaries without implementing workflow DAGs or later reliability features.

## Context

Phase 0 created the monorepo, Spring Boot modules, frontend shell, and Docker Compose infrastructure. Phase 1 adds the smallest useful durable product behavior: a tenant-scoped no-op task that is created through HTTP, stored in PostgreSQL, dispatched through Kafka, and completed by the worker.

## Prerequisites

- Java 25 available locally.
- Docker Desktop running.
- PostgreSQL, Kafka, Redis, control-plane, scheduler, and worker available through Docker Compose.

## Files/Components Affected

- `backend/taskforge-domain`
- `backend/taskforge-control-plane`
- `backend/taskforge-scheduler`
- `backend/taskforge-worker`
- `contracts/openapi`
- `contracts/events`
- `docs`

## Implementation Steps

- Add Flyway migration `V2__phase1_task_execution.sql`.
- Add `TaskExecution`, enums, and repository in the shared domain module.
- Add control-plane no-op task create/read API.
- Add scheduler pending-task claim and Kafka dispatch.
- Add worker Kafka listener and transactional completion service.
- Add unit tests around state transitions and service boundaries.
- Verify the live path with Docker Compose.

## Expected Architecture Changes

The backend now has a real cross-service execution path, but still avoids workflow runs, DAGs, transactional outbox, durable leases, retries, and idempotent inbox records.

## Tests Required

- Backend Maven reactor verification.
- Unit tests for domain state transitions, control-plane service/controller behavior, scheduler claim behavior, worker listener delegation, and worker completion behavior.
- Docker Compose health checks.
- Live API smoke from task creation to `SUCCEEDED`.

## Acceptance Criteria

- `POST /api/tasks/noop` creates a durable `PENDING` task.
- Scheduler dispatches one pending task and increments `attempt_count`.
- Kafka topic `taskforge.task-dispatch.v1` exists.
- Worker consumes dispatch and marks the task `SUCCEEDED`.
- `GET /api/tasks/{id}` returns the tenant-scoped final task state.
- No Phase 2 DAG behavior is implemented.

## Verification Commands

- `cd backend && .\mvnw.cmd verify`
- `docker compose config`
- `docker compose up --build -d`
- Actuator health checks on ports `8080`, `8081`, and `8082`
- `POST /api/tasks/noop`
- Poll `GET /api/tasks/{id}` until `SUCCEEDED`
- PostgreSQL query against `flyway_schema_history` and `task_execution`

## Risks

- The current dispatch message is a temporary UUID string, not a versioned event envelope.
- There is no transactional outbox yet, so database state and Kafka dispatch can diverge on process failure.
- There are no durable worker leases yet.
- Duplicate delivery handling is minimal and based on ignoring tasks no longer in `DISPATCHED`.

## Rollback/Recovery Considerations

- Revert Phase 1 source changes and remove migration `V2` only in a disposable development database.
- For local development, `make reset` can recreate Docker volumes.
- Never edit an applied production Flyway migration; add a new migration instead.

## Completion Checklist

- [x] Flyway migration added.
- [x] Control-plane API added.
- [x] Scheduler dispatch added.
- [x] Worker completion added.
- [x] Tests added.
- [x] Docker Compose smoke passed.
- [x] Documentation and contracts updated.
