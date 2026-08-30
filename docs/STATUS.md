# Status

Last updated: 2026-08-30

## Latest Completed Milestone

M4 - Workflow Product UI.

## Completed

- M0 foundation and one-task vertical slice are complete and pushed to `main`.
- M1 workflow DAG engine is complete and pushed to `main`.
- M2 reliable distributed execution is complete and pushed to `main`.
- M3 identity, organization tenancy, RBAC foundations, refresh-token rotation, and API protection are implemented.
- M4 workflow product UI is implemented for authenticated workflow management, draft editing, DAG visualization, validation, publishing, run creation, and run tracking.
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
- The frontend stores login sessions locally, restores sessions with refresh-token rotation, and calls authenticated workflow APIs.
- The workflow console can list organization-scoped workflows, create workflows, edit draft `NO_OP` nodes and edges, save/validate/publish drafts, start runs, and poll run/task status.
- Run detail shows a graph-oriented execution view plus task/run status summaries.

## Known Limitations

- Additional task handlers, schedules, observability, and cloud deployment remain later milestones.
- M3 supports one active organization per token. Invitations, organization switching, account recovery, account lockout, audit logs, and API keys remain later work.
- M2 keeps retry/dead-letter policy intentionally simple; richer operator controls and observability remain later milestones.
- M4 uses polling for live run updates because the backend SSE stream remains a later enhancement.
- Approval UX is a placeholder surface only; actual manual approval task execution belongs to M5.

## Verification Notes

- M4 focused backend/frontend tests passed during implementation. See latest task output for full command results.

## Next Milestone

M5 - Automation Capabilities.
