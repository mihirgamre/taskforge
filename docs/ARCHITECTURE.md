# Architecture

TaskForge is a multi-tenant workflow orchestration platform. The repository implements M0-M7: workflow DAG execution, reliable distributed execution, identity/tenancy, product UI, automation handlers, local observability, and cloud/portfolio readiness.

## Responsibilities

- Control plane: public API, authentication, organization-scoped workflow management, draft/version/run APIs, approvals, API keys, schedules, rate limiting, and dashboard reads.
- Scheduler: transactional outbox publication, due schedule materialization, pending task dispatch, retry timing, and expired lease recovery.
- Worker: Kafka consumption, durable inbox records, task lease acquisition, handler execution, task completion/failure persistence, retry/dead-letter transitions, and workflow progression.

## Local Architecture

```mermaid
flowchart TB
  FE["React frontend"] --> CP["Control plane"]
  CP --> PG[("PostgreSQL")]
  CP --> R[("Redis")]
  SCH["Scheduler"] --> PG
  SCH --> K[("Kafka")]
  W["Worker"] --> K
  W --> PG
  Prom["Prometheus"] --> CP
  Prom --> SCH
  Prom --> W
  Graf["Grafana"] --> Prom
```

## Storage And Messaging

PostgreSQL is the durable source of truth for organizations, users, workflow definitions, workflow versions, runs, task executions, schedules, worker leases, attempts, outbox records, inbox records, and dead-letter records.

Kafka is an asynchronous transport. It does not own business state. Duplicate delivery is expected and handled through durable inbox records plus task lease/state checks.

Redis is non-authoritative infrastructure for rate limiting and future acceleration. Redis loss must not destroy workflow state.

## Frontend/Backend Interaction

The React console calls the control plane over relative `/api/*` URLs. Locally, nginx proxies those calls to the control plane. In the AWS blueprint, CloudFront serves frontend assets from S3 and routes `/api/*` to the public ALB.

## Execution Lifecycle

```mermaid
sequenceDiagram
  participant User
  participant Control as Control Plane
  participant DB as PostgreSQL
  participant Scheduler
  participant Kafka
  participant Worker
  User->>Control: Save, validate, publish workflow
  Control->>DB: Persist immutable version
  User->>Control: Start workflow run
  Control->>DB: Create run and task executions
  Scheduler->>DB: Claim pending tasks and write outbox events
  Scheduler->>Kafka: Publish unpublished outbox events
  Worker->>Kafka: Consume dispatch event
  Worker->>DB: Record inbox, acquire lease, execute handler
  Worker->>DB: Complete task and unblock eligible children
  Control->>DB: Read run/task status for UI polling
```

## AWS Direction

The M7 deployment blueprint targets CloudFront/S3 for frontend assets, CloudFront `/api/*` routing to an ALB, ECS Fargate for control-plane/scheduler/worker, RDS PostgreSQL, ElastiCache Redis, managed Kafka/MSK supplied by bootstrap brokers, Secrets Manager/KMS, ECR, CloudWatch, and future OpenTelemetry export.

```mermaid
flowchart LR
  User --> CF["CloudFront"]
  CF --> S3["S3 frontend"]
  CF --> ALB["ALB /api/*"]
  ALB --> CP["ECS control-plane"]
  SCH["ECS scheduler"] --> PG[("RDS PostgreSQL")]
  CP --> PG
  W["ECS worker"] --> PG
  SCH --> MSK[("MSK / managed Kafka")]
  W --> MSK
  CP --> REDIS[("ElastiCache Redis")]
  CP --> SM["Secrets Manager"]
  SCH --> CW["CloudWatch Logs"]
  W --> CW
```

## Consistency And Reliability

TaskForge provides at-least-once event delivery, transactional outbox publication, idempotent consumers, durable worker leases, explicit state-transition rules, retry backoff, and dead-letter records. It does not claim exactly-once business processing.

## Security Boundaries

Every tenant-scoped resource is authorized through organization membership. Secrets must be stored in managed secret systems, never committed, and never returned after creation. Logs and task outputs avoid authentication material and HTTP response bodies.
