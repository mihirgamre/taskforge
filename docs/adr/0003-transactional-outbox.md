# ADR 0003: Transactional Outbox

## Status

Accepted

## Decision

Use a transactional outbox for database state changes that require Kafka publication.

## Consequences

Business state and outbox records are written in one PostgreSQL transaction. Publication is retried until acknowledged, and consumers must tolerate duplicate events.

