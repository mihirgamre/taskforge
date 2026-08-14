# Testing Strategy

## Tests That Currently Exist

- Backend unit/context tests for common/domain modules and service bootstraps.
- Frontend React Testing Library smoke test for the application shell.
- Playwright smoke test configuration for later browser verification.

## Planned Tests

- Unit tests for workflow graph validation, state transitions, retry calculations, permission matrix, idempotency, schedule calculations, and output limits.
- Spring MVC, security, JPA, and serialization slice tests.
- Testcontainers integration tests for PostgreSQL, Kafka, Redis, and Mailpit/MailHog.
- End-to-end Playwright tests for critical user journeys.
- Concurrency tests for duplicate requests, multiple schedulers, multiple workers, worker death, lease recovery, duplicate Kafka delivery, and dependency outages.
- Security tests for invalid/expired tokens, CSRF, CORS, brute force, XSS, SSRF, SQL injection, cross-tenant access, role escalation, and secret leakage.
- k6 load tests with documented environment and actual measured results.

Coverage is a guardrail. Do not use coverage numbers as a substitute for testing the reliability and security properties in the specification.

