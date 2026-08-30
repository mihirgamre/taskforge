# Interview Notes Template

Use this structure for future major features. Do not fabricate learning, measurements, or results.

Feature:

Problem:

Why the problem matters:

Our solution:

Execution flow:

Why we chose this design:

Alternatives considered:

Trade-offs:

Failure modes:

How we tested it:

Measured results:

Important files/classes:

Likely interview questions:

My explanation in plain English:

---

# Phase 1 - One-Task Vertical Execution Slice

Feature:
Minimal no-op task execution across the control plane, scheduler, Kafka, worker, and PostgreSQL.

Problem:
Before building workflow DAGs, TaskForge needs proof that the core service boundaries can move one durable piece of work through the system.

Why the problem matters:
Most risk in this project is not drawing workflow boxes. It is durable state, safe background dispatch, duplicate delivery, and honest failure handling.

Our solution:
The control plane creates a tenant-scoped `NO_OP` task in PostgreSQL. The scheduler claims a pending task in a transaction, marks it `DISPATCHED`, and publishes its id to Kafka. The worker consumes that id and marks the task `SUCCEEDED`.

Execution flow:
`POST /api/tasks/noop` creates `PENDING`. Scheduler locks the oldest pending row and changes it to `DISPATCHED`. Kafka carries the task id to the worker. Worker completes only tasks currently in `DISPATCHED`. `GET /api/tasks/{id}` reads the task by id and tenant header.

Why we chose this design:
PostgreSQL is the durable source of truth, so task state survives process and Kafka restarts. Kafka decouples work discovery from work execution and keeps the worker boundary asynchronous without introducing workflow DAG complexity yet.

Alternatives considered:
A synchronous control-plane-to-worker call would be simpler but would not prove the event-driven worker boundary. An immediate transactional outbox would be more reliable but belongs to the reliability phase and would add more concepts before the basic slice is proven.

Trade-offs:
The slice is intentionally small. It does not include workflow runs, leases, retries, idempotent inbox records, user authentication, or a versioned event envelope.

Failure modes:
If Kafka publish fails synchronously after a task is marked `DISPATCHED`, the scheduler returns the task to `PENDING` unless it has already reached a terminal state. If the scheduler crashes after committing `DISPATCHED` but before publishing to Kafka, Phase 1 can leave the task stuck. Phase 3 transactional outbox will solve this by storing the outbound event in the same database transaction as the state change and retrying publication until acknowledged.

How we tested it:
Unit and MVC tests cover task creation, retrieval, bad API input, state transitions, scheduler no-work behavior, scheduler rollback behavior, worker duplicate delivery behavior, malformed task ids, missing task ids, and wrong task states. A PostgreSQL/Testcontainers test verifies two simultaneous scheduler claims do not claim the same task. Docker Compose smoke tests verify the live API-to-database-to-Kafka-to-worker path.

Measured results:
No performance measurements exist yet. The verified result is functional correctness for the Phase 1 smoke path.

Important files/classes:
`TaskExecution`, `TaskExecutionRepository`, `TaskCommandService`, `TaskController`, `TaskClaimService`, `TaskDispatchService`, `NoOpTaskWorker`, `TaskCompletionService`, and Flyway migration `V2__phase1_task_execution.sql`.

Likely interview questions:
Why is PostgreSQL the source of truth instead of Kafka? What prevents two schedulers from claiming one task? What happens on duplicate Kafka delivery? What happens if publish fails after the database commit? Why not implement the outbox immediately? How would Phase 3 change this design?

My explanation in plain English:
Phase 1 proves the smallest reliable path through the backend. The database owns truth, Kafka moves work between services, and each service owns one clear responsibility. The design is intentionally not finished: the direct publish failure window is real and documented, and the next reliability phase will close it with a transactional outbox and idempotent consumers.

---

# M1 - Workflow DAG Engine

Feature:
Versioned workflow DAG execution for `NO_OP` nodes.

Problem:
A workflow engine must run nodes in dependency order rather than treating every task as independent.

Our solution:
TaskForge stores workflow drafts, published versions, nodes, edges, runs, and node task executions in PostgreSQL. Published versions are immutable so a historical run always points to the exact graph it used.

Execution flow:
Publishing validates the draft graph with Kahn's algorithm. Starting a run creates one task per node. Root nodes become `PENDING`; non-root nodes remain `BLOCKED`. The existing scheduler/Kafka/worker path dispatches pending tasks. When a worker succeeds, the completion transaction checks child dependencies and unlocks children whose direct predecessors are all `SUCCEEDED`.

Why we chose this design:
The graph algorithm is deterministic and unit-testable without HTTP or a database. PostgreSQL constraints protect durable graph and task integrity. A run-level pessimistic lock serializes dependency activation so concurrent fan-in predecessor completions cannot schedule the same child twice.

Trade-offs:
M1 keeps reliability intentionally basic. It does not add retries, leases, idempotent inbox records, dead-letter handling, or transactional outbox; those remain M2.

Failure modes:
If a required task fails in M1, the run is marked `FAILED` and descendants stay blocked. If the scheduler crashes after marking a task `DISPATCHED` but before Kafka publish, the known M0/M1 direct-publish failure window remains until M2.

How we tested it:
Domain tests cover valid linear, fan-out, fan-in, multiple-root DAGs and invalid cycles, self-edges, duplicate edges, missing nodes, duplicate node keys, empty workflows, and published-version immutability. PostgreSQL-backed execution tests exist for linear, fan-out, fan-in, duplicate child scheduling prevention, workflow completion, and failure semantics, but they require Docker/Testcontainers to run.

Measured results:
No performance measurements exist yet.

Likely interview questions:
What is a DAG? Why make workflow versions immutable? How are root nodes identified? How does fan-in avoid early execution? What prevents duplicate child scheduling? What remains for the reliability milestone?

My explanation in plain English:
A workflow is a directed acyclic graph: arrows describe which tasks must finish before others can start. TaskForge validates that graph before publishing, stores the published version permanently, and then creates runnable task rows only when dependencies are satisfied.

---

# M2 - Reliable Distributed Execution

Feature:
Transactional dispatch reliability, idempotent worker consumption, worker leases, retry scheduling, and dead-letter task records.

Problem:
M0/M1 could leave a task stuck if the scheduler committed `DISPATCHED` and crashed before Kafka publish. Duplicate Kafka delivery also needed durable consumer tracking.

Our solution:
The scheduler now writes a task-dispatch outbox event in the same PostgreSQL transaction that marks a task `DISPATCHED`. A separate outbox publisher retries Kafka publication until it is acknowledged. Workers consume JSON dispatch envelopes with stable event IDs, record inbox rows per consumer, acquire PostgreSQL task leases, and only complete tasks with the matching lease token.

Execution flow:
`PENDING` task -> scheduler lock -> `DISPATCHED` plus `outbox_event` -> outbox publisher sends Kafka message -> worker records `inbox_event` -> worker lease acquired -> task succeeds or is retried/dead-lettered.

Trade-offs:
The outbox publisher currently performs Kafka send while holding the selected outbox row lock. This is simple and correct for the milestone, but can be optimized later if publisher throughput becomes a bottleneck.

Failure modes:
If Kafka publish fails, the outbox row remains `PENDING` with backoff. If a worker crashes after leasing a task, scheduler lease recovery returns it to `PENDING` while attempts remain. Exhausted attempts are marked `FAILED` and recorded in `dead_letter_task`.

How we tested it:
Focused unit tests cover outbox event creation, outbox publish success/failure, idempotent inbox behavior, lease acquisition, lease heartbeats, lease-token completion, retry backoff, and dead-letter recording. Full backend verification passed with Testcontainers, and Docker Compose smoke tests exercised linear, fan-out, and fan-in workflows through outbox, Kafka, inbox, leases, and completion.

Measured results:
No performance measurements exist yet.

Likely interview questions:
What failure window does the outbox close? Why still claim at-least-once instead of exactly-once? How does the inbox prevent duplicate business effects? Why are leases stored in PostgreSQL? What happens when a worker lease expires?

---

# M3 - Identity, Tenancy, and API Protection

Feature:
Authenticated organization-scoped API access.

Problem:
The earlier tenant header was a development placeholder. A workflow platform needs identity, durable organization ownership, role checks, and session security before product UI or public API expansion.

Our solution:
Registration creates a user, organization, and owner membership. Passwords are BCrypt-hashed. Login issues a short-lived JWT access token and a durable refresh token stored only as a hash. Refresh tokens rotate on use, and reuse of an old token revokes the token family.

Execution flow:
Clients register or log in, then call protected `/api/**` endpoints with `Authorization: Bearer ...`. The JWT filter validates the signature and builds an authenticated principal containing user id, organization id, and role. Task and workflow services use that organization id in repository queries.

Why we chose this design:
JWT access tokens keep API requests stateless, while refresh-token rows provide durable rotation and reuse detection. Organization ownership is stored in PostgreSQL with the rest of the durable application state.

Trade-offs:
M3 intentionally supports one active organization per token and a simple read/write role split. Invitations, organization switching, account lockout, MFA, audit logging, and API keys remain later work.

Failure modes:
If Redis is unavailable, rate limiting fails open and logs a warning. If an access token is stolen, it remains usable until expiry; immediate access-token revocation is not implemented yet.

How we tested it:
PostgreSQL-backed integration tests cover registration, login, refresh rotation, refresh-token reuse rejection, authenticated task creation, current-user context, and cross-organization workflow isolation. A unit test covers Redis rate-limit rejection.

Measured results:
No performance measurements exist yet.

Likely interview questions:
Why not trust a tenant header? Why hash refresh tokens? What happens on refresh-token reuse? Why return 404 for cross-tenant reads? What remains before production-grade auth?

My explanation in plain English:
M3 makes tenancy come from authentication instead of client-provided headers. Every protected request carries a signed token, the backend derives the organization from that token, and database queries include that organization so one customer cannot read another customer’s workflows.
