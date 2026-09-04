# Portfolio Notes

TaskForge demonstrates a production-minded distributed workflow platform without claiming unverified production traffic.

## What Is Implemented

- Authenticated organization-scoped workflow APIs.
- Durable workflow definitions, immutable versions, runs, task executions, leases, retries, inbox/outbox records, and schedules.
- React workflow console with DAG editing, publishing, run tracking, approvals, and task results.
- Local PostgreSQL, Kafka, Redis, Prometheus, Grafana, and k6 smoke testing.
- AWS ECS/Fargate Terraform blueprint and hardened GitHub Actions workflows.

## Verified Local Claims

- Backend verification passes with the Maven reactor.
- Frontend lint, typecheck, unit tests, and production build pass.
- Docker Compose stack starts locally.
- k6 smoke exercises registration, workflow creation, draft update, publish, and run start.

Do not add resume metrics such as throughput, latency SLOs, uptime, or cost until measured against a deployed environment.

## Demo Script

1. Start `docker compose --profile observability up --build`.
2. Open `http://localhost:5173`.
3. Register or sign in.
4. Create a workflow with `START -> BUILD`, `START -> NOTIFY`, `BUILD -> FINISH`, and `NOTIFY -> FINISH`.
5. Save, validate, publish, and run.
6. Show the run graph reaching `SUCCEEDED`.
7. Open Prometheus/Grafana to show service metrics are available.
