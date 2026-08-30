# ADR 0011 - Auth-Backed Organization Tenancy

## Status

Accepted.

## Context

Earlier milestones used an `X-Tenant-Id` development header for the standalone task API and did not protect workflow APIs. M3 needs a real security boundary without implementing future API keys, SSO, or advanced account lifecycle features.

## Decision

TaskForge will use email/password registration with BCrypt password hashes, stateless HMAC-signed JWT access tokens, durable refresh tokens with rotation, and organization memberships for tenant context. Control-plane services derive the active organization from the authenticated principal, not from caller-supplied tenant headers.

Redis-backed rate limiting protects `/api/**` requests. Redis remains non-authoritative: if Redis is unavailable, the request is allowed and the outage is logged.

## Consequences

- New workflows, workflow runs, workflow tasks, and no-op tasks are organization-scoped.
- Cross-organization reads return `404` to avoid leaking resource existence.
- Refresh-token reuse is rejected and revokes the token family.
- Access tokens are simple bearer tokens; token revocation before expiry remains a future hardening topic.
- The current implementation supports one active organization per token. Organization switching and invitations can be added later without changing scheduler or worker boundaries.
