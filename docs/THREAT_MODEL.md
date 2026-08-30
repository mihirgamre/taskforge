# Threat Model

## Assets

- User identities and sessions.
- Organization-scoped workflow definitions and runs.
- Organization secrets and API keys.
- Audit history and execution logs.
- Durable workflow state in PostgreSQL.

## Expected Threats

- Cross-tenant data access.
- Token theft, refresh-token reuse, and brute-force login.
- Role escalation.
- Secret leakage through responses, logs, traces, task outputs, or errors.
- SSRF through future HTTP task handlers.
- SQL injection and unsafe query construction.
- XSS in rendered workflow data and logs.
- Duplicate event delivery causing duplicate business effects.
- Worker crash or lease loss causing stuck or duplicate task execution.
- Redis or Kafka outage affecting reliability.
- Oversized payloads and unbounded logs causing resource exhaustion.

## Implemented Mitigations

- No arbitrary code, shell, or user container execution exists.
- No secret material is present in repository defaults.
- Phase 1 task reads are scoped by task id plus `X-Tenant-Id`.
- Authenticated organization context scopes new workflow and task resources.
- Refresh-token hashes and rotation reduce impact of refresh-token database exposure and reuse.
- Basic RBAC prevents viewer roles from write operations.
- Redis-backed rate limiting limits request bursts while preserving availability if Redis is down.
- TaskForge does not execute arbitrary user code or external HTTP calls.

## Planned Mitigations

- Organization invitations, switching, and stricter per-feature permissions.
- Audit logging for security-sensitive events.
- Request validation, payload limits, and explicit error models.
- Secret encryption, masking, and redaction tests.
- Restricted HTTP destination validation for future HTTP tasks.
- CORS policy hardening for deployed frontend origins.
