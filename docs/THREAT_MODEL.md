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
- No product endpoints exist yet beyond foundation metadata.

## Planned Mitigations

- Tenant authorization at service boundaries and repository queries.
- Transactional outbox, processed-event records, and idempotent consumers.
- Durable worker leases in PostgreSQL.
- Request validation, payload limits, and explicit error models.
- Secret encryption, masking, and redaction tests.
- Restricted HTTP destination validation for future HTTP tasks.
- Security headers, CORS/CSRF controls, and rate limits.

