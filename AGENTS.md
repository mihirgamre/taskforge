# AGENTS.md

TaskForge is a multi-tenant distributed workflow orchestration platform. The master requirements live in `docs/TASKFORGE_SPEC.md`; do not duplicate them here.

## Repository Map

- `backend/` contains the Java/Spring Boot Maven modules.
- `frontend/` contains the React/TypeScript/Vite application.
- `contracts/` contains API and event schemas.
- `docs/` contains architecture, roadmap, decisions, security, threat model, testing, and execution plans.
- `infra/` contains Docker, observability, and Terraform assets.
- `load-tests/` contains k6 scenarios.

## Working Rules

- Inspect existing code and documentation before modifying files.
- Keep changes scoped to the requested task and avoid unrelated refactors.
- Update architecture documentation and ADRs after significant architectural changes.
- Use Flyway migrations for database evolution; never rely on Hibernate schema generation outside tests.
- Do not commit secrets, credentials, environment files, build outputs, logs, dependency directories, or generated coverage.
- Never fabricate test results, health checks, performance metrics, or security claims.
- Preserve tenant isolation and document any code path that handles tenant-scoped data.
- Preserve documented at-least-once event delivery, transactional outbox, idempotent consumer, and worker-lease semantics.
- Add regression tests for bug fixes.
- Document significant trade-offs in `docs/DECISIONS.md` or an ADR.

## Build And Test

- Backend: `cd backend && ./mvnw verify` or `.\mvnw.cmd verify` on Windows.
- Frontend: `cd frontend && npm ci && npm run lint && npm run typecheck && npm test && npm run build`.
- Local stack: `docker compose up --build`.

Do not report a command as passing unless it was actually run and the output confirmed success.

