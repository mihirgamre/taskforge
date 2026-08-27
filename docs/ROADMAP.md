# Roadmap

`docs/TASKFORGE_SPEC.md` remains the complete long-term product specification. This roadmap groups implementation into practical milestones without dropping requirements.

## M0 - Foundation + Vertical Slice

Status: COMPLETE.

Includes original Phases 0-1: monorepo foundation, backend/frontend foundations, Docker Compose, CI skeleton, and the API -> PostgreSQL -> scheduler -> Kafka -> worker -> PostgreSQL completion path for one no-op task.

## M1 - Workflow DAG Engine

Status: COMPLETE.

Adds workflow definitions, drafts, versions, nodes, edges, DAG validation, cycle detection, immutable published versions, workflow runs bound to published versions, dependency-aware task eligibility, multi-node execution, workflow completion aggregation, and basic failure aggregation.

## M2 - Reliable Distributed Execution

Combines original Phases 3-5: transactional outbox, reliable Kafka publication, idempotent consumers/inbox, worker leases, heartbeats, expired lease recovery, retries, exponential backoff, dead-letter handling, and distributed concurrency testing.

## M3 - Identity, Tenancy And API Protection

Includes authentication, refresh-token rotation, organizations, membership, RBAC, tenant isolation, Redis-backed rate limiting, and API security.

## M4 - Workflow Product UI

Includes visual workflow builder, workflow management UI, execution graph, run details, live execution updates, approval UX, and responsive/error/loading states.

## M5 - Automation Capabilities

Includes task handlers, schedules, cron/timezone handling, API keys, workflow API triggers, manual approvals, safe HTTP tasks, transforms, and notification/report tasks.

## M6 - Production Reliability

Includes OpenTelemetry, Prometheus, Grafana, structured logging, security hardening, resilience testing, threat-model validation, k6 load testing, performance optimization, and measured results.

## M7 - Cloud + Portfolio Completion

Includes AWS, Terraform, ECS/Fargate, RDS, managed Redis/Kafka approach, CI/CD hardening, vulnerability scanning, README, diagrams, screenshots/demo, final interview notes, and verified resume metrics.

## Old Phase Mapping

- M0: Phases 0-1
- M1: Phase 2
- M2: Phases 3-5
- M3: Phases 6 and 10 security/rate-limiting work
- M4: Phases 7 and 11
- M5: Phases 8-9 plus approvals and trigger capabilities
- M6: Phases 12-14
- M7: Phases 15-17 plus CI/CD hardening from Phase 16
