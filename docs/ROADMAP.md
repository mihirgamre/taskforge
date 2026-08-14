# Roadmap

## Phase 0 - Repository And Development Foundation

Create the professional monorepo, documentation, dependency decisions, backend/frontend foundations, Docker Compose, CI skeleton, and verification baseline.

## Phase 1 - One-Task Vertical Execution Slice

Status: implemented as a deliberately smaller slice than the long-term workflow target.

Implemented: create one tenant-scoped no-op task through the control-plane API, persist it in PostgreSQL, dispatch it through the scheduler to Kafka, complete it in the worker, and read final state through the API.

Deferred to later phases: workflow versions, run records, DAG execution, transactional outbox, durable leases, idempotent inbox records, and frontend result display.

## Phase 2 - Workflow DAG Engine

Add graph modelling, validation, immutable publishing, state transitions, and dependency unlocking.

## Phase 3 - Kafka Reliability, Transactional Outbox, Idempotency

Harden outbox publisher, consumer inbox, event IDs, correlation/causation IDs, schema versions, and duplicate-delivery tests.

## Phase 4 - Distributed Workers And Leases

Support concurrent workers, durable leases, heartbeats, expired-lease recovery, and concurrency tests.

## Phase 5 - Retries And Dead-Letter Handling

Add retry policies, backoff, jitter, attempt records, poison-message handling, and dead-letter views.

## Phase 6 - Authentication, Organizations, RBAC

Add registration, login, refresh-token rotation, organizations, membership roles, audit logging, and cross-tenant authorization tests.

## Phase 7 - Visual Workflow Builder

Build the editable graph UI, validation feedback, version history, and conflict handling.

## Phase 8 - Additional Task Handlers

Add restricted handlers such as delay, JSON transform, conditional, manual approval, email simulation, report generation, and restricted HTTP.

## Phase 9 - Scheduling And API Triggers

Add one-time and cron schedules, time-zone handling, API keys, scopes, and idempotent trigger requests.

## Phase 10 - Redis Caching And Rate Limiting

Use Redis for non-authoritative caching, rate limiting, and fast idempotency checks with database fallback.

## Phase 11 - Live Execution UI

Add SSE updates, run graph status, approvals inbox, cancellation, retry actions, and dead-letter inspection.

## Phase 12 - Observability

Add OpenTelemetry, Prometheus, Grafana dashboards, structured logs, correlation IDs, and local alerts.

## Phase 13 - Security Hardening

Finish SSRF protections, CSRF/CORS/security headers, secret encryption, redaction tests, dependency scanning, and threat-model review.

## Phase 14 - Performance And Load Testing

Create k6 scenarios and document actual measured results without fabricating targets.

## Phase 15 - AWS Deployment

Add Terraform for ECS Fargate, RDS, Redis, Kafka, S3/CloudFront, KMS, Secrets Manager, and cost-conscious demo settings.

## Phase 16 - CI/CD And Production Hardening

Add image builds, SBOMs, vulnerability scanning, migration safety checks, smoke tests, protected deployments, and rollback behavior.

## Phase 17 - Portfolio Polish

Finalize README, diagrams, screenshots, demo plan, honest metrics, known limitations, and resume-ready claims backed by evidence.
