# Status

Last updated: 2026-08-14

## Current Phase

Phase 1 - one-task vertical execution slice.

## Implemented

- Master product specification stored at `docs/TASKFORGE_SPEC.md`.
- Monorepo directory structure.
- Backend Maven multi-module foundation for control plane, scheduler, and worker service boundaries.
- Frontend React/TypeScript/Vite shell.
- Local Docker Compose infrastructure definition for PostgreSQL, Redis, Kafka, backend services, and frontend.
- Initial architecture, dependency, roadmap, security, threat model, testing, interview-notes, and ADR documentation.
- Minimal GitHub Actions CI workflow.
- Minimal one-task execution slice:
  - `POST /api/tasks/noop` creates a tenant-scoped `NO_OP` task in PostgreSQL.
  - Scheduler claims the oldest `PENDING` task, marks it `DISPATCHED`, and publishes the task id to Kafka topic `taskforge.task-dispatch.v1`.
  - Worker consumes the dispatch message and marks the task `SUCCEEDED`.
  - `GET /api/tasks/{id}` reads task state scoped by `X-Tenant-Id`.
- Shared Flyway migrations now live in `taskforge-domain` so control plane, scheduler, and worker validate the same schema.

## Not Implemented Yet

- Authentication, organizations, RBAC, workflow CRUD, DAG execution, durable transactional outbox, worker leases, retries, dead-letter handling, live updates, secrets, audit logs, load testing, observability dashboards, and AWS infrastructure.

## Verification Notes

Backend checks pass:

- `cd backend && .\mvnw.cmd verify`
- Latest Phase 1 backend verification after adversarial review: 32 tests, 0 failures, 0 errors, 0 skipped.

Frontend checks pass:

- `cd frontend && npm.cmd ci`
- `npm.cmd run lint`
- `npm.cmd run typecheck`
- `npm.cmd test -- --run`
- `npm.cmd run build`
- `npm.cmd audit --audit-level=moderate`

Docker Compose checks pass:

- `docker compose config`
- `docker compose up --build -d`
- `docker compose ps`
- `docker compose exec -T postgres pg_isready -U taskforge -d taskforge`
- `docker compose exec -T redis redis-cli ping`
- `docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list`
- HTTP health checks for control plane, scheduler, and worker all returned status `UP`.
- Frontend returned HTTP 200 on `http://localhost:5173/`.
- Phase 1 live smoke passed on 2026-08-14:
  - Created task `ff65492f-171f-4802-b22f-0dcad75b3e8e`.
  - Observed `PENDING -> DISPATCHED -> SUCCEEDED`.
  - Durable row recorded `attempt_count = 1`, `dispatched_at`, and `completed_at`.

## Adversarial Review Notes

- Defect fixed: terminal tasks can no longer be moved back to `PENDING` by a late scheduler rollback path.
- Added explicit API handling for malformed JSON, missing tenant headers, invalid UUID path values, and `ResponseStatusException`.
- Added PostgreSQL/Testcontainers concurrency verification proving two simultaneous scheduler claims do not claim the same task.
- Current documented limitation remains: Phase 1 does not have a transactional outbox. If the scheduler commits `DISPATCHED` and the process dies before Kafka publish, the task can remain stuck until Phase 3 introduces outbox/reconciliation.

GitHub repository exists at `https://github.com/mihirgamre/taskforge`. Phase 1 changes are not committed yet.
