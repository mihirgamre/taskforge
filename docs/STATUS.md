# Status

Last updated: 2026-08-27

## Latest Completed Milestone

M1 - Workflow DAG Engine.

## Completed

- M0 foundation and one-task vertical slice are complete and pushed to `main`.
- Existing Phase 1 path remains: `POST /api/tasks/noop` -> PostgreSQL `PENDING` task -> scheduler claim -> Kafka dispatch -> worker completion -> PostgreSQL `SUCCEEDED`.

## Implemented

- Workflow definitions with editable drafts and immutable published versions.
- Workflow nodes and edges for `NO_OP` tasks.
- Deterministic DAG validation using Kahn's algorithm.
- Validation for empty graphs, duplicate nodes, duplicate edges, self-edges, missing node references, and cycles.
- Workflow runs bound to a published workflow version.
- Task executions linked to workflow run/node references.
- Root nodes start `PENDING`; dependent nodes start `BLOCKED`.
- Worker completion unlocks children only after all direct predecessors succeed.
- Workflow runs aggregate to `SUCCEEDED` after all tasks succeed, or `FAILED` after a required task fails.

## Known Limitations

- M1 does not implement M2 reliability features: transactional outbox, idempotent inbox, worker leases, retries, dead-letter handling, or recovery for the existing database-to-Kafka crash window.
- Authentication, organizations, RBAC, Redis rate limiting, visual workflow builder, additional task handlers, schedules, observability, and cloud deployment remain later milestones.

## Verification Notes

- `cd backend && .\mvnw.cmd verify`: passed, 48 backend tests.
- `docker compose config --quiet`: passed.
- `docker compose up --build -d`: passed.
- Health checks for control-plane, scheduler, and worker returned `UP`.
- Live DAG smoke tests passed for linear `A -> B -> C`, fan-out `A -> {B, C}`, and fan-in `{A, B} -> C`.
- PostgreSQL verification confirmed each smoke-test task reached `SUCCEEDED` with one attempt.

## Next Milestone

M2 - Reliable Distributed Execution. M2 is ready to begin after the M1 commit, but no M2 functionality has been started.
