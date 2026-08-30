# Status

Last updated: 2026-08-29

## Latest Completed Milestone

M3 - Identity, Tenancy, and API Protection.

## Completed

- M0 foundation and one-task vertical slice are complete and pushed to `main`.
- M1 workflow DAG engine is complete and pushed to `main`.
- M2 reliable distributed execution is complete and pushed to `main`.
- M3 identity, organization tenancy, RBAC foundations, refresh-token rotation, and API protection are implemented.
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
- Registration and login create BCrypt-hashed users, organizations, and owner memberships.
- JWT access tokens carry authenticated user, organization, and role context.
- Refresh tokens are stored hashed, rotate on refresh, and reject reused revoked tokens.
- Control-plane task and workflow APIs use authenticated organization context instead of caller-supplied tenant headers.
- Cross-organization workflow access returns not found.
- Redis-backed rate limiting protects `/api/**` requests with fail-open behavior if Redis is unavailable.

## Known Limitations

- Visual workflow builder, additional task handlers, schedules, observability, and cloud deployment remain later milestones.
- M3 supports one active organization per token. Invitations, organization switching, account recovery, account lockout, audit logs, and API keys remain later work.
- M2 keeps retry/dead-letter policy intentionally simple; richer operator controls and observability remain later milestones.

## Verification Notes

- M3 focused control-plane tests passed before full verification. See latest task output for full command results.

## Next Milestone

M4 - Workflow Product UI.
