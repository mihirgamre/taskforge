# Security

## Already Implemented

- `.gitignore` excludes common secret, environment, log, dependency, and build-output files.
- `.env.example` contains development-only placeholder values.
- Control plane permits only the foundation endpoint and health checks anonymously; all other endpoints require authentication once implemented.
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

No production authentication, authorization, secret storage, task execution, or tenant isolation code exists yet. Those controls are planned, not implemented.

