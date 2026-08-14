# ADR 0007: Server-Sent Events For Live Updates

## Status

Accepted

## Decision

Prefer Server-Sent Events over WebSockets for live workflow-run updates.

## Consequences

The initial live-update model remains simpler because the browser primarily receives server-originated status events. WebSockets can be reconsidered if bidirectional collaboration becomes necessary.

