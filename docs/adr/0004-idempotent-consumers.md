# ADR 0004: Idempotent Consumers

## Status

Accepted

## Decision

Consumers must record processed event identifiers and make state transitions idempotent.

## Consequences

Every event needs a stable event ID, schema version, correlation ID, and causation ID. Duplicate Kafka delivery must not create duplicate business effects.

