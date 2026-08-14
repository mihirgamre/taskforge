# Security

## Already Implemented

- `.gitignore` excludes common secret, environment, log, dependency, and build-output files.
- `.env.example` contains development-only placeholder values.
- Control plane exposes the Phase 1 no-op task endpoints without authentication for local development.
- Phase 1 task reads require `X-Tenant-Id` and query by both task id and tenant id.
- Phase 1 API errors return sanitized error bodies for malformed JSON, invalid UUIDs, missing tenant headers, and missing tasks.
- Control plane disables generated default Spring Security credentials in Phase 1 so startup logs do not contain a development password.
- Documentation explicitly forbids fabricated security claims and secret commits.

## Planned

- Secure registration/login, password hashing, JWT access tokens, refresh-token rotation, reuse detection, logout, and account lockout.
- Organization membership authorization and role-based access checks.
- Cross-tenant authorization tests for every tenant-scoped resource.
- CSRF, CORS, and security headers appropriate to the deployed auth model.
- API key hashing, scoping, prefix display, and revocation.
- Secret encryption at rest using authenticated encryption locally and KMS/Secrets Manager adapters in AWS.
- Log, trace, and task-output redaction.
- SSRF protections for restricted HTTP tasks.
- Rate limiting and brute-force protection.
- Dependency, secret, and container vulnerability scanning in CI.

## Current Limits

No production authentication, organization membership, RBAC, secret storage, or cryptographic tenant authorization exists yet. Phase 1 tenant isolation is limited to explicit tenant-header scoping in the task API and repository query; it is not a production security boundary.
