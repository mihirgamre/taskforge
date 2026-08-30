# Decisions

This file summarizes current accepted architectural decisions. Full records live in `docs/adr/`.

- PostgreSQL is the durable source of truth.
- Kafka is used for asynchronous task dispatch and integration between scheduler/worker concerns.
- Event delivery is at least once, not exactly once.
- Database-to-Kafka publication will use a transactional outbox.
- Consumers must be idempotent and record processed event identifiers.
- Redis is non-authoritative infrastructure.
- Worker leases will be stored durably in PostgreSQL.
- Server-Sent Events are preferred over WebSockets for one-way run updates.
- Backend uses Spring Boot with a Maven multi-module layout.
- Frontend uses React, TypeScript, and Vite.
- Deployment direction is ECS Fargate rather than Kubernetes.
- Phase 0 intentionally excludes product workflow behavior.
- Phase 1 intentionally implements a no-op task slice before workflow runs/DAGs so the API, PostgreSQL, scheduler, Kafka, and worker boundaries are proven with minimal domain complexity.
- Shared schema migrations live in `taskforge-domain` so every backend service validates the same Flyway migration history.
- M3 replaces development tenant headers with auth-backed organization tenancy using BCrypt password hashes, JWT access tokens, durable rotating refresh tokens, membership roles, and Redis-backed rate limiting.
