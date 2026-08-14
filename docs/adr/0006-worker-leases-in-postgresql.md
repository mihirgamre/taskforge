# ADR 0006: Worker Leases In PostgreSQL

## Status

Accepted

## Decision

Store worker leases durably in PostgreSQL with lease owner, token hash, expiration, heartbeat, and optimistic concurrency metadata.

## Consequences

Lease recovery can survive worker crashes and Redis restarts. Claim queries must be concurrency-safe.

