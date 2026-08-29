# Status

Last updated: 2026-08-29

## Latest Completed Milestone

M2 - Reliable Distributed Execution.

## Completed

- M0 foundation and one-task vertical slice are complete and pushed to `main`.
- M1 workflow DAG engine is complete and pushed to `main`.
- M2 reliable distributed execution is implemented and locally verified.
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
- Scheduler dispatch uses a PostgreSQL transactional outbox.
- Outbox publisher retries Kafka publication until events are marked `PUBLISHED`.
- Worker consumption records durable inbox rows keyed by event and consumer.
- Workers acquire PostgreSQL task leases and complete tasks only with the matching lease token.
- Retry backoff, expired-lease recovery, and durable dead-letter task records are in place.

## Known Limitations

- Authentication, organizations, RBAC, Redis rate limiting, visual workflow builder, additional task handlers, schedules, observability, and cloud deployment remain later milestones.
- M2 keeps retry/dead-letter policy intentionally simple; richer operator controls and observability remain later milestones.

## Verification Notes

- `cd backend && .\mvnw.cmd verify`: passed, 49 backend tests.
- `docker compose config --quiet`: passed.
- `docker --context default compose up --build -d`: passed.
- Health checks for control-plane, scheduler, and worker returned `UP`.
- Live DAG smoke tests passed for linear `A -> B -> C`, fan-out `A -> {B, C}`, and fan-in `{A, B} -> C` through the outbox/Kafka/inbox/lease path.
- PostgreSQL verification confirmed 9 smoke-test tasks reached `SUCCEEDED` with one attempt, leases cleared, 9 outbox rows `PUBLISHED`, 9 inbox rows recorded, and 0 dead-letter rows.

## Next Milestone

M3 - Identity, Tenancy, and API Protection.
