# ADR 0001: PostgreSQL As Durable Source Of Truth

## Status

Accepted

## Decision

Use PostgreSQL as the durable source of truth for tenant data, workflow definitions, workflow runs, task state, attempts, scheduling state, worker leases, idempotency records, outbox records, inbox records, and audit history.

## Consequences

Kafka and Redis cannot be the only location for business-critical state. PostgreSQL schema changes must use Flyway migrations.

