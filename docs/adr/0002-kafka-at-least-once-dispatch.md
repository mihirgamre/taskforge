# ADR 0002: Kafka For At-Least-Once Task Dispatch

## Status

Accepted

## Decision

Use Kafka for asynchronous task dispatch between scheduler/control-plane responsibilities and workers. Delivery semantics are at least once.

## Consequences

Consumers must be idempotent. Documentation and portfolio claims must not describe exactly-once business processing.

