# Testing Strategy

## Tests That Currently Exist

- Backend unit/context tests for common/domain modules and service bootstraps.
- Backend unit and MVC tests for Phase 1 task creation, tenant-scoped lookup, nonexistent tasks, malformed JSON, missing tenant headers, invalid UUIDs, pending-task claim/dispatch state transition, scheduler no-work behavior, Kafka listener delegation, malformed dispatch IDs, missing task records, wrong task states, duplicate delivery, and worker completion state transition.
- PostgreSQL/Testcontainers integration test for concurrent scheduler claims and Flyway migration startup.
- Frontend React Testing Library smoke test for the application shell.
- Playwright smoke test configuration for later browser verification.
- Common observability tests for correlation ID propagation and body/query-free request logging behavior.
- Worker test coverage that HTTP task execution persists only status metadata, not response bodies.
- k6 smoke load script for authenticated workflow creation, draft update, publish, and run start.
- Manual Docker Compose smoke for Phase 1:
  - Actuator health checks for control plane, scheduler, and worker.
  - `POST /api/tasks/noop` followed by polling `GET /api/tasks/{id}` until `SUCCEEDED`.
  - PostgreSQL inspection of Flyway version 2 and `task_execution` state.

## Planned Tests

- Unit tests for additional output limits and future handler-specific failure behavior.
- Spring MVC, security, JPA, and serialization slice tests.
- Additional Testcontainers integration tests for Kafka, Redis, and Mailpit/MailHog where useful. Phase 1 currently uses a real PostgreSQL concurrency test plus Docker Compose smoke verification for the cross-service Kafka path.
- End-to-end Playwright tests for critical user journeys.
- Concurrency tests for duplicate requests, multiple schedulers, multiple workers, worker death, lease recovery, duplicate Kafka delivery, and dependency outages.
- Security tests for invalid/expired tokens, CSRF, CORS, brute force, XSS, SSRF, SQL injection, cross-tenant access, role escalation, and secret leakage.
- Larger k6 load tests with documented environment and actual measured results after deployment sizing.

Coverage is a guardrail. Do not use coverage numbers as a substitute for testing the reliability and security properties in the specification.
