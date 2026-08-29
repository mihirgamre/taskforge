# M2 Reliable Distributed Execution Plan

## Objective

Close the M0/M1 dispatch reliability gap and make worker processing safe under at-least-once Kafka delivery.

## Scope

- Transactional outbox for task dispatch events.
- Outbox publisher with retry/backoff.
- Durable inbox records keyed by event and consumer.
- PostgreSQL task leases with owner, token, expiration, and heartbeat timestamp.
- Expired lease recovery.
- Retry scheduling with bounded attempts.
- Durable dead-letter records for exhausted tasks.

## Out Of Scope

- Authentication, tenancy model changes, Redis rate limiting, visual workflow UI, additional task handlers, observability stack, AWS, and performance metrics.

## Verification

- Focused scheduler/worker unit tests.
- Full backend `.\mvnw.cmd verify` when Docker/Testcontainers is reachable.
- Docker Compose build/startup.
- Live workflow smoke test proving dispatch through outbox, Kafka, inbox, lease, and completion.

## Current State

Implementation is complete and locally verified. Full backend `verify` passed with Testcontainers, Docker Compose build/startup passed, and live workflow smoke tests verified dispatch through outbox, Kafka, inbox, leases, and completion.
