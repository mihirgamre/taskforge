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

