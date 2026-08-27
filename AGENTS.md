# AGENTS.md

TaskForge is a multi-tenant distributed workflow orchestration platform. The complete long-term product target lives in `docs/TASKFORGE_SPEC.md`; do not duplicate it here.

## Context Routing

For normal implementation work, read only the minimum relevant context in this order:

1. `AGENTS.md`
2. `docs/STATUS.md`
3. current active execution plan, if one exists
4. ADRs directly relevant to the current milestone
5. source code and tests directly relevant to the current milestone

Do not automatically read the entire spec, all ADRs, all architecture docs, all security/testing docs, or completed execution plans. Consult `docs/TASKFORGE_SPEC.md` only when current milestone requirements are ambiguous or missing.

## Repository Map

- `backend/`: Java/Spring Boot Maven modules for control plane, scheduler, worker, and shared domain.
- `frontend/`: React/TypeScript/Vite application.
- `contracts/`: API and event schemas.
- `docs/`: status, roadmap, decisions, architecture notes, and interview notes.
- `infra/`, `load-tests/`, `.github/`: infrastructure, performance tests, and CI/CD assets.

## Development Workflow

For a normal milestone, one Codex task should handle implementation, relevant tests, focused review of the changed diff, confirmed-defect fixes, concise documentation updates, commit, and push.

Use separate planning/review tasks only when a major architecture decision is unresolved, a dangerous migration is involved, distributed/concurrency correctness needs special investigation, a major security design changes, or the user explicitly asks.

## Working Rules

- Inspect existing code before modifying it.
- Keep changes scoped; avoid unrelated refactors.
- Use Flyway for database evolution; do not rely on Hibernate schema creation outside tests.
- Do not commit secrets, credentials, `.env` files, build outputs, logs, dependency directories, generated coverage, or database volumes.
- Never fabricate test results, health checks, performance metrics, or security claims.
- Preserve tenant isolation and documented event-delivery semantics.
- Add regression tests for confirmed bugs.
- Document significant trade-offs in `docs/DECISIONS.md`, an ADR, or concise milestone docs.

## Testing

During implementation, run tests relevant to changed components. Before committing a completed milestone, run the complete verification appropriate to that milestone. Do not repeatedly rerun large unchanged suites without a reason.

## Documentation

Update only documentation affected by the current milestone. `docs/STATUS.md` should concisely state the completed milestone, implemented capabilities, known limitations, and next milestone.

## Final Responses

Keep normal completion responses under 500 words. Report only major functionality, important decisions, meaningful defects found/fixed, test results, commit hash, push result, and blockers.
