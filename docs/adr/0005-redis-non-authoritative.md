# ADR 0005: Redis As Non-Authoritative Infrastructure

## Status

Accepted

## Decision

Use Redis for caching, rate limiting, and acceleration only. Redis will not own durable workflow state.

## Consequences

Redis outages may degrade performance or disable non-critical acceleration, but they must not lose workflow state.

