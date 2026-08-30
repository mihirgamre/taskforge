# M3 - Identity, Tenancy, and API Protection

## Objective

Implement authenticated API access, organization-backed tenancy, basic RBAC, refresh-token rotation, and Redis-backed rate limiting without changing scheduler, worker, DAG, or reliability semantics.

## Scope

- Add identity, organization, membership, and refresh-token persistence.
- Protect control-plane APIs with stateless bearer tokens.
- Replace placeholder tenant headers with authenticated organization context.
- Enforce organization scoping for workflows, workflow runs, workflow tasks, and no-op tasks.
- Add minimal auth endpoints for register, login, refresh, logout, and current-user context.
- Add focused tests for token rotation, API protection, tenant isolation, and rate limiting.

## Out Of Scope

- Visual workflow UI, API keys, advanced account recovery, SSO, account lockout, audit logging, secret storage, and M4+ functionality.

## Verification

- Run focused control-plane tests during implementation.
- Run full backend verification before commit.
- Validate Docker Compose configuration and run an authenticated smoke test when the stack is available.

## Acceptance Criteria

- Anonymous API access is rejected except auth endpoints and health/info.
- New workflows, runs, and tasks are scoped to the authenticated user's organization.
- Cross-organization reads return not found rather than leaking existence.
- Refresh tokens rotate and reuse of a revoked token is rejected.
- Redis rate limiting can block excessive requests and is documented as currently simple.
