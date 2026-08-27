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
