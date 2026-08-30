# Security

## Already Implemented

- `.gitignore` excludes common secret, environment, log, dependency, and build-output files.
- `.env.example` contains development-only placeholder values.
- Control plane exposes the Phase 1 no-op task endpoints without authentication for local development.
- Control-plane API endpoints require bearer-token authentication except registration, login, refresh, health, and info.
- Passwords are stored as BCrypt hashes.
- JWT access tokens carry user, organization, and membership role context.
- Refresh tokens are stored as hashes, rotate on refresh, and reject reuse of revoked/expired tokens.
- New task and workflow resources are scoped by authenticated organization id rather than a caller-supplied tenant header.
- Cross-organization workflow access returns not found.
- Redis-backed rate limiting protects `/api/**` requests with development defaults.
- Phase 1 task reads now query by authenticated organization id.
- Phase 1 API errors return sanitized error bodies for malformed JSON, invalid UUIDs, missing tenant headers, and missing tasks.
- Control plane disables generated default Spring Security credentials in Phase 1 so startup logs do not contain a development password.
- Documentation explicitly forbids fabricated security claims and secret commits.

## Planned

- Account lockout, invitation flows, organization switching, and password recovery.
- Finer-grained role permissions beyond the current read/write split.
- Cross-tenant authorization tests for every tenant-scoped resource.
- CSRF, CORS, and security headers appropriate to the deployed auth model.
- API key hashing, scoping, prefix display, and revocation.
- Secret encryption at rest using authenticated encryption locally and KMS/Secrets Manager adapters in AWS.
- Log, trace, and task-output redaction.
- SSRF protections for restricted HTTP tasks.
- Stronger brute-force controls and audit logging.
- Dependency, secret, and container vulnerability scanning in CI.

## Current Limits

Access-token revocation before expiry, MFA, account recovery, invitation workflows, API keys, audit logs, and encrypted user-managed secrets are not implemented yet. Redis rate limiting currently fails open if Redis is unavailable so Redis does not become a hard dependency for API availability.
