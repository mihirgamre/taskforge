# Event Contracts

Event contracts will be formalized when versioned event envelopes are introduced.

Current Phase 1 dispatch topic:

- Topic: `taskforge.task-dispatch.v1`
- Producer: scheduler
- Consumer group: `taskforge-worker`
- Key: task id UUID string
- Value: task id UUID string

This minimal message shape is intentionally temporary. Phase 3 should replace it with an explicit schema containing event id, tenant id, task id, event type, schema version, correlation id, causation id, and creation timestamp.
