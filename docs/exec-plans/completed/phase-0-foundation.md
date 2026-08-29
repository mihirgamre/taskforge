# Phase 0 Execution Plan

## Objective

Create a professional TaskForge repository and development foundation without implementing workflow product features.

## Context

The master specification is stored at `docs/TASKFORGE_SPEC.md`. It is the long-term target, not a mandate to implement every feature in Phase 0.

## Prerequisites

- Java 25 for backend verification.
- Node.js 24 and npm for frontend verification.
- Docker with Compose support for local stack verification.
- GitHub CLI authentication only if remote repository creation is required.

## Files And Components Affected

- Root repository controls and documentation.
- `backend/` Maven modules.
- `frontend/` React/Vite project.
- `compose.yaml` and Dockerfiles.
- `contracts/`, `infra/`, `load-tests/`, and `.github/workflows/`.

## Implementation Steps

1. Copy the full master specification into `docs/TASKFORGE_SPEC.md`.
2. Create root repository hygiene files.
3. Add backend Maven multi-module skeleton.
4. Add frontend strict TypeScript shell.
5. Add Docker Compose infrastructure.
6. Add architecture, roadmap, security, testing, dependency, and ADR documentation.
7. Add CI skeleton.
8. Run applicable verification commands.

## Expected Architecture Changes

The repo gains a modular-monolith style service foundation with three independently runnable service entry points: control plane, scheduler, and worker.

## Tests Required

- Backend Maven verify.
- Frontend install, lint, typecheck, tests, and build.
- Docker Compose configuration validation.
- Docker local startup and health checks where supported.

## Acceptance Criteria

- Repository structure exists.
- Documentation accurately separates implemented and planned controls.
- Backend and frontend foundations build in a correctly provisioned environment.
- No product workflow behavior is implemented.

## Verification Commands

```bash
git status --short --branch
cd backend && ./mvnw verify
cd frontend && npm ci && npm run lint && npm run typecheck && npm test -- --run && npm run build
docker compose config
docker compose up --build
```

## Risks

- Local machine may not have Java 25 or Maven wrapper JAR.
- Docker Compose may not be installed or Docker credentials/config may block execution.
- Latest dependency versions may expose ecosystem compatibility gaps.

## Rollback And Recovery

Remove the generated foundation files before the first commit if the structure needs to be recreated. After commit, use normal Git revert rather than destructive reset.

## Completion Checklist

- [x] Root files created.
- [x] Master spec copied.
- [x] Backend foundation created.
- [x] Frontend foundation created.
- [x] Docker Compose created.
- [x] Docs and ADRs created.
- [x] CI created.
- [x] Verification completed and recorded.

## Verification Results

- Backend Maven `verify` passes with Java 25.0.4.
- Frontend install, lint, typecheck, unit tests, production build, and npm audit pass.
- `docker compose config` validates the Compose file.
- `docker compose up --build -d` builds and starts PostgreSQL, Redis, Kafka, control plane, scheduler, worker, and frontend.
- PostgreSQL, Redis, and Kafka direct probes pass.
- Control plane, scheduler, and worker actuator health endpoints return `UP`.
- Frontend returns HTTP 200 locally.
