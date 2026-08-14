# MASTER BUILD PROMPT: TASKFORGE

You are acting as the principal software engineer, backend architect, frontend engineer, database engineer, security engineer, DevOps engineer, SRE and QA lead for this project.

Build a complete, production-minded application named **TaskForge**.

TaskForge is a multi-tenant distributed workflow orchestration platform. Users can visually create workflows composed of connected tasks, publish immutable workflow versions, trigger workflow runs manually or on a schedule, monitor execution in real time, retry failed work, approve manual steps, and inspect execution history and operational metrics.

This is a serious portfolio project intended to demonstrate:

* Java and Spring Boot backend engineering
* Distributed job processing
* Event-driven architecture
* PostgreSQL data modelling and concurrency control
* Apache Kafka event streaming
* Redis caching, rate limiting and idempotency
* Reliable background workers
* Retries, leases and failure recovery
* Transactional outbox and idempotent consumers
* Secure multi-tenant authentication and authorization
* React and TypeScript frontend development
* Docker and local infrastructure
* Automated testing
* CI/CD
* Observability
* Performance and load testing
* AWS deployment and infrastructure as code
* Production-quality documentation

Treat TaskForge as though real organizations and users will depend on it. Do not implement knowingly unsafe shortcuts simply because this is a portfolio project.

---

# 1. WORKING RULES

## 1.1 General behaviour

Before changing code:

1. Inspect the entire repository.
2. Identify existing files, conventions and unfinished work.
3. Do not overwrite functional code without understanding it.
4. Create or update:

   * `docs/STATUS.md`
   * `docs/ROADMAP.md`
   * `docs/DECISIONS.md`
5. Write an implementation plan tied to the phases in this specification.
6. Begin implementation after planning. Do not stop after producing only a plan.

Continue through the phases autonomously where the environment allows.

Stop only when:

* Credentials or secrets are required.
* A destructive cloud action requires explicit approval.
* A product requirement is genuinely impossible to infer.
* The execution environment prevents further work.
* Context limitations require stopping at a clean milestone.

Do not ask unnecessary preference questions. Make sensible engineering decisions, document them and proceed.

## 1.2 Quality requirements

Never:

* Claim a test passed without running it.
* Claim the application starts without verifying it.
* Leave fake implementations that return hard-coded success.
* Hide errors with empty catch blocks.
* disable tests simply to make the build pass.
* Add `TODO` comments in place of essential functionality.
* Store secrets in source control.
* log passwords, tokens, secret values or complete sensitive payloads.
* use arbitrary code execution for workflow tasks.
* use `eval`, dynamic Java compilation or unrestricted shell commands.
* use a fake in-memory database for production code.
* silently ignore failed Kafka messages.
* claim “exactly once” processing when the implementation provides at-least-once delivery.
* create microservices that have no independent responsibility.
* add technology solely for résumé keywords without using it meaningfully.

Every completed phase must include:

* Working implementation
* Automated tests
* Documentation
* Local run instructions
* Error handling
* Security review
* Verification commands
* A concise summary of remaining work

## 1.3 Version policy

Use stable, mutually compatible versions and pin them.

Baseline:

* Java 25 LTS
* Spring Boot 4.1.x
* Maven Wrapper
* PostgreSQL 18.x
* Apache Kafka 4.x using KRaft mode
* Redis 8.x
* React 19.x
* TypeScript
* Vite
* Current active-LTS Node.js
* Docker and the current Compose specification

Before installing dependencies:

1. Verify compatibility.
2. Prefer stable releases over snapshots, release candidates or beta releases.
3. Record selected versions in `docs/DEPENDENCIES.md`.
4. Commit lockfiles and wrapper files.
5. Explain any deviation from the baseline.

Use an OpenJDK distribution suitable for development and container deployment.

---

# 2. PRODUCT OVERVIEW

TaskForge allows organizations to automate repeatable processes.

Example workflow:

```text
Receive request
      ↓
Validate payload
      ↓
Generate report
      ↓
Manual approval
      ↓
Send webhook
      ↓
Send confirmation email
```

A workflow consists of nodes connected as a directed acyclic graph.

Users should be able to:

* Register and sign in securely.
* Create or join an organization.
* Invite organization members.
* Create workflow drafts.
* Add, configure, move and connect workflow nodes.
* Validate workflow structure.
* Publish immutable workflow versions.
* Trigger a workflow manually.
* Schedule one-time or recurring runs.
* Trigger a workflow through an authenticated API endpoint.
* Follow execution progress in near real time.
* Inspect task attempts, logs, output and errors.
* Cancel eligible workflow runs.
* Retry failed workflow runs or individual failed tasks where safe.
* Approve or reject manual-approval steps.
* View organization-level usage and reliability metrics.
* Manage organization secrets without exposing their values.
* View security and workflow audit history.

---

# 3. IMPORTANT NON-GOALS

Do not implement the following in the initial release:

* Arbitrary user-supplied code execution
* Shell command execution
* Docker-container execution submitted by users
* Real financial transactions
* Real payment information
* Unrestricted network requests
* A full Airflow, Temporal or AWS Step Functions clone
* Kubernetes merely for résumé value
* Multi-region active-active infrastructure
* Complex billing
* Public marketplace for workflow templates
* Mobile applications
* Generative-AI functionality unrelated to workflow execution

The application should be ambitious but finishable.

---

# 4. ARCHITECTURE

Use a monorepo with independently runnable services.

Recommended structure:

```text
taskforge/
├── README.md
├── LICENSE
├── .editorconfig
├── .gitignore
├── .env.example
├── compose.yaml
├── Makefile
├── docs/
│   ├── STATUS.md
│   ├── ROADMAP.md
│   ├── DECISIONS.md
│   ├── DEPENDENCIES.md
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── DATA_MODEL.md
│   ├── SECURITY.md
│   ├── THREAT_MODEL.md
│   ├── TESTING.md
│   ├── OPERATIONS.md
│   ├── LOAD_TEST_REPORT.md
│   └── adr/
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/
│   ├── taskforge-domain/
│   ├── taskforge-common/
│   ├── taskforge-control-plane/
│   ├── taskforge-scheduler/
│   ├── taskforge-worker/
│   └── taskforge-notification-worker/
├── frontend/
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.ts
│   ├── src/
│   └── tests/
├── contracts/
│   ├── openapi/
│   └── events/
├── infra/
│   ├── docker/
│   ├── observability/
│   └── terraform/
│       ├── modules/
│       ├── environments/demo/
│       └── environments/production/
├── scripts/
├── load-tests/
│   └── k6/
└── .github/
    ├── workflows/
    └── dependabot.yml
```

## 4.1 Service responsibilities

### Control Plane

The control-plane service owns:

* Authentication
* Refresh-token management
* Organization membership
* Role-based access control
* Workflow definitions
* Workflow versions
* Workflow validation
* Workflow run creation
* Manual task approvals
* Run cancellation requests
* API keys
* Organization secrets
* Audit logs
* Read APIs for dashboard and frontend
* Server-sent events for live run updates
* Transactional outbox records

### Scheduler

The scheduler owns:

* Finding scheduled workflows whose next execution time has arrived
* Creating scheduled workflow runs
* Finding task executions eligible for dispatch
* Safely claiming due work
* Writing dispatch events through the transactional outbox
* Calculating retry times
* Recovering expired worker leases
* Marking permanently failed work as dead-lettered
* Computing future recurring schedule times
* Preventing duplicate schedule firing

### Worker

Workers own:

* Consuming task-ready events
* Atomically claiming task executions
* Maintaining worker leases and heartbeats
* Executing supported task handlers
* Recording task attempts
* Producing success or failure events
* Applying task-level timeouts
* Redacting secrets
* Ensuring application-level idempotency
* Recovering safely from duplicate Kafka delivery

Multiple worker instances must be able to run concurrently.

### Notification Worker

The notification worker owns:

* Simulated or development email delivery
* Email-verification messages
* Password-reset messages
* Workflow-completion notifications
* Manual-approval notifications
* Dead-letter alerts
* Notification retry handling

Use Mailpit or MailHog locally. Support Amazon SES through an adapter in production, but do not require AWS to run locally.

## 4.2 Shared modules

### Domain module

Contains:

* Domain entities
* Value objects
* State machines
* Domain validation
* Workflow graph validation
* Retry calculations
* Domain events
* No framework-specific controller code

### Common module

Contains only genuinely shared infrastructure:

* Error model
* Correlation IDs
* Logging utilities
* Serialization configuration
* Authentication principals
* Event-envelope definitions
* Time abstraction
* Test fixtures where appropriate

Do not turn the common module into an unstructured dumping ground.

---

# 5. DELIVERY AND CONSISTENCY MODEL

## 5.1 Source of truth

PostgreSQL is the durable source of truth for:

* Workflow definitions
* Workflow versions
* Workflow runs
* Task execution state
* Task attempts
* Scheduling
* Worker leases
* Audit history
* Idempotency records
* Outbox and inbox records

Kafka transports events but is not the sole authority for workflow state.

Redis must not be the only durable location for business-critical state.

## 5.2 Delivery guarantee

Implement and document:

* At-least-once event delivery
* Idempotent event consumption
* Idempotent task handling where possible
* Transactional outbox for database-to-Kafka publication
* Consumer inbox or processed-event records
* Unique event identifiers
* Correlation IDs
* Causation IDs
* Schema versioning

Do not claim true exactly-once business processing.

## 5.3 Transactional outbox

Whenever a database state change requires an event:

1. Update business state.
2. Insert an outbox record in the same PostgreSQL transaction.
3. An outbox publisher reads unpublished records.
4. Publish the event to Kafka.
5. Mark the outbox record as published only after broker acknowledgement.
6. Retry failed publication safely.
7. Use a unique event ID.
8. Ensure repeated publication does not cause repeated business effects.

Use `SELECT ... FOR UPDATE SKIP LOCKED` or another documented safe claim mechanism so multiple publisher instances can operate concurrently.

## 5.4 Consumer idempotency

Each consumer must:

1. Read the event ID.
2. Check or insert an inbox/processed-event record.
3. Perform the state transition and inbox insertion transactionally where possible.
4. Ignore already processed events.
5. Record malformed or permanently invalid events.
6. Route poison messages to a dead-letter topic after the configured attempts.

---

# 6. DOMAIN MODEL

Use UUID primary keys.

Store all machine timestamps as UTC using timezone-aware PostgreSQL types. Accept and return ISO-8601 timestamps. Store schedule time zones separately using IANA time-zone identifiers.

Every multi-tenant business table must include `organization_id` unless ownership is indirect and enforced through a parent.

Do not depend on user-supplied organization IDs without authorization checks.

## 6.1 Users

Suggested fields:

```text
users
- id
- email
- normalized_email
- password_hash
- display_name
- email_verified_at
- status
- failed_login_count
- locked_until
- created_at
- updated_at
- last_login_at
- version
```

Requirements:

* Case-insensitive unique email.
* Normalize email consistently.
* Do not expose password hashes.
* Support `ACTIVE`, `LOCKED` and `DISABLED`.
* Use optimistic locking where useful.

## 6.2 Refresh tokens

```text
refresh_tokens
- id
- user_id
- token_hash
- token_family_id
- issued_at
- expires_at
- rotated_at
- revoked_at
- replaced_by_token_id
- created_ip_hash
- user_agent_hash
```

Requirements:

* Store only a secure hash of the token.
* Rotate refresh tokens.
* Detect refresh-token reuse.
* Revoke the complete token family after detected reuse.
* Allow user logout from one session or all sessions.

## 6.3 Organizations and memberships

```text
organizations
- id
- name
- slug
- status
- created_by
- created_at
- updated_at
- version

organization_memberships
- id
- organization_id
- user_id
- role
- status
- invited_by
- invited_at
- accepted_at
- created_at
- updated_at
```

Roles:

* `OWNER`
* `ADMIN`
* `MEMBER`
* `VIEWER`

Create an explicit permission matrix and enforce it in backend services.

Examples:

* OWNER: all organization actions, including ownership transfer.
* ADMIN: manage workflows, runs, members and secrets, but not delete the owner.
* MEMBER: create and operate workflows.
* VIEWER: read-only access.

## 6.4 Workflow definitions and versions

```text
workflows
- id
- organization_id
- name
- slug
- description
- status
- latest_draft_version
- latest_published_version
- created_by
- updated_by
- created_at
- updated_at
- archived_at
- version
```

```text
workflow_versions
- id
- workflow_id
- version_number
- lifecycle_status
- graph_hash
- validation_status
- published_at
- published_by
- created_at
- created_by
```

Rules:

* Draft versions can be edited.
* Published versions are immutable.
* Running workflows always reference an exact published version.
* Publishing validates the graph, node configurations and referenced secrets.
* The graph receives a deterministic hash.
* Re-publishing identical content should not create unnecessary versions unless explicitly requested.

## 6.5 Nodes and edges

```text
workflow_nodes
- id
- workflow_version_id
- node_key
- type
- name
- description
- configuration_json
- position_x
- position_y
- timeout_seconds
- max_attempts
- retry_policy_json
- continue_on_failure
- created_at
```

```text
workflow_edges
- id
- workflow_version_id
- source_node_key
- target_node_key
- edge_type
- condition_json
- created_at
```

Constraints:

* Unique node key per workflow version.
* Source and target must exist in the same version.
* No self-edge.
* No duplicate edge.
* Graph must be acyclic.
* Published graph must contain at least one node.
* Enforce maximum graph limits, such as 100 nodes and 250 edges for the initial release.
* Validate that all non-root nodes have valid incoming dependencies.
* Provide clear validation errors tied to node IDs.

## 6.6 Workflow runs

```text
workflow_runs
- id
- organization_id
- workflow_id
- workflow_version_id
- trigger_type
- trigger_reference
- idempotency_key
- status
- input_json
- output_json
- requested_by
- scheduled_for
- started_at
- completed_at
- cancellation_requested_at
- failure_code
- failure_summary
- created_at
- updated_at
- version
```

Statuses:

* `PENDING`
* `RUNNING`
* `PAUSED`
* `SUCCEEDED`
* `FAILED`
* `CANCELLATION_REQUESTED`
* `CANCELLED`
* `DEAD_LETTERED`

Define an explicit allowed-transition map and unit test every transition.

## 6.7 Task executions and attempts

```text
task_executions
- id
- organization_id
- workflow_run_id
- workflow_node_id
- node_key
- status
- available_at
- attempt_count
- max_attempts
- lease_owner
- lease_token_hash
- lease_expires_at
- heartbeat_at
- input_json
- output_json
- last_error_code
- last_error_summary
- started_at
- completed_at
- created_at
- updated_at
- version
```

Statuses:

* `BLOCKED`
* `READY`
* `QUEUED`
* `RUNNING`
* `WAITING_APPROVAL`
* `RETRY_SCHEDULED`
* `SUCCEEDED`
* `FAILED`
* `TIMED_OUT`
* `CANCELLED`
* `SKIPPED`
* `DEAD_LETTERED`

```text
task_attempts
- id
- task_execution_id
- attempt_number
- worker_id
- status
- started_at
- completed_at
- duration_ms
- error_category
- error_code
- redacted_error_message
- output_summary_json
- trace_id
- created_at
```

Do not store unlimited logs or unbounded task outputs in the primary tables.

Apply configured size limits and truncate only with an explicit indicator.

## 6.8 Scheduling

```text
workflow_schedules
- id
- organization_id
- workflow_id
- workflow_version_id
- schedule_type
- cron_expression
- timezone
- one_time_at
- next_fire_at
- last_fire_at
- enabled
- misfire_policy
- created_by
- created_at
- updated_at
- version
```

Support:

* One-time schedules
* Recurring cron schedules
* Time-zone-aware scheduling
* Enable/disable
* Misfire policy:

  * `SKIP`
  * `FIRE_ONCE`
* Prevention of duplicate schedule execution
* Correct handling of daylight-saving transitions

Use `Instant` internally and explicit `ZoneId` handling.

## 6.9 Organization secrets

```text
organization_secrets
- id
- organization_id
- name
- encrypted_value
- encryption_key_version
- created_by
- created_at
- updated_at
- rotated_at
- version
```

Requirements:

* Secret names are unique within an organization.
* Values are encrypted at rest.
* Values are never returned after creation.
* Display only metadata and a masked indicator.
* Secrets can be referenced by workflow nodes.
* Secret values must be redacted from logs, errors, traces and task outputs.
* Use a local development encryption key from environment variables.
* Design a production adapter for AWS KMS or Secrets Manager.
* Do not invent encryption primitives. Use authenticated encryption through established libraries.

## 6.10 API keys

```text
api_keys
- id
- organization_id
- name
- key_prefix
- key_hash
- scopes
- created_by
- created_at
- expires_at
- last_used_at
- revoked_at
```

Requirements:

* Show the complete API key once at creation.
* Store only the hash.
* Use a visible non-secret prefix for identification.
* Support scopes.
* Support expiration and revocation.
* Rate-limit API-key endpoints.
* Record use in audit logs.

## 6.11 Operational tables

Include:

* `outbox_events`
* `inbox_events`
* `idempotency_keys`
* `audit_logs`
* `dead_letter_records`
* `notification_deliveries`
* `worker_instances`

Create useful indexes for:

* Organization-scoped queries
* Due schedules
* Ready tasks
* Expired leases
* Unpublished outbox events
* Workflow-run status
* Time-ordered history
* Idempotency lookup
* Event-ID lookup

Use Flyway for every schema change.

Never use automatic Hibernate schema creation outside tests.

---

# 7. WORKFLOW GRAPH ENGINE

## 7.1 Validation

Implement deterministic graph validation:

* Detect cycles using a suitable graph algorithm.
* Identify root nodes.
* Validate unreachable nodes.
* Validate duplicate node keys.
* Validate missing source or target nodes.
* Validate task-specific configuration.
* Validate secret references.
* Validate maximum size.
* Validate conditional branches.
* Return all meaningful validation errors, not only the first.
* Ensure validation results are stable and testable.

## 7.2 Dependency execution

A node becomes eligible when:

* All required predecessor nodes have reached an acceptable terminal state.
* Its branch condition, if present, evaluates to true.
* The workflow run has not been cancelled.
* It has not already reached a terminal state.

When a node succeeds:

1. Persist its result.
2. Determine newly eligible child nodes.
3. Transition them to `READY`.
4. Write dispatch outbox events.
5. Update the workflow-run state if all terminal conditions are satisfied.

When a node fails:

* Apply its retry policy.
* If retryable and attempts remain, schedule a retry.
* If permanently failed, apply `continue_on_failure`.
* Mark downstream tasks `SKIPPED` when their required dependency cannot succeed.
* Determine whether the entire workflow must fail.
* Preserve a clear error chain.

## 7.3 Workflow completion

A workflow succeeds only when:

* All required tasks succeeded, or
* Optional tasks failed under an explicit continue-on-failure policy, and
* No required task remains active.

A workflow fails when:

* A required task permanently fails.
* A task is dead-lettered.
* Graph execution reaches an invalid state.
* A configured workflow timeout is reached.

Implement reconciliation logic that can safely recompute a run’s aggregate status if an event is delayed or duplicated.

---

# 8. SUPPORTED TASK TYPES

Implement task handlers through a clean interface such as:

```java
public interface TaskHandler {
    TaskType supportedType();
    TaskExecutionResult execute(TaskExecutionContext context);
    ValidationResult validateConfiguration(JsonNode configuration);
}
```

Handlers must be independently unit-testable.

## 8.1 Delay task

Configuration:

* Duration
* Optional description

Behaviour:

* Do not keep a worker thread sleeping for long periods.
* Store the future availability time.
* Scheduler re-enqueues the task when due.
* Validate maximum duration.

## 8.2 HTTP request task

Configuration:

* HTTP method
* URL
* Headers
* Query parameters
* Request body template
* Timeout
* Accepted status codes
* Secret references

Security requirements:

* Allow only `https` in production.
* Block loopback addresses.
* Block private and link-local IP ranges unless explicitly allowed in local development.
* Block cloud instance metadata addresses.
* Resolve DNS safely and protect against DNS rebinding where feasible.
* Limit redirects or disable them by default.
* Revalidate redirect destinations.
* Limit request and response sizes.
* Apply connection and read timeouts.
* Permit only supported methods.
* Redact authorization headers and secrets.
* Do not store unlimited response bodies.
* Add organization-level destination allow-lists.
* Provide a test-only mock HTTP service in local Compose.
* Categorize 4xx and 5xx failures appropriately.
* Retry only safe or explicitly configured operations.
* Support idempotency headers for downstream services.

## 8.3 JSON transform task

Provide safe declarative transformation.

Support a limited transformation model such as:

* Select value by JSON path.
* Rename fields.
* Add constants.
* Remove fields.
* Construct a new JSON object from known paths.
* Perform limited string and numeric operations.

Do not use JavaScript evaluation, expression-language execution with arbitrary method access or dynamic code.

Validate mappings before publication.

## 8.4 Conditional task

Support safe predicates:

* equals
* not equals
* greater than
* greater than or equal
* less than
* less than or equal
* exists
* does not exist
* contains
* starts with
* ends with

Conditions may inspect prior task outputs through controlled references.

Do not allow arbitrary code.

## 8.5 Manual approval task

Behaviour:

* Transition to `WAITING_APPROVAL`.
* Display approval details in the frontend.
* Allow authorized members to approve or reject.
* Record approver identity, timestamp and optional comment.
* Prevent double approval.
* Make approval idempotent.
* Support approval expiry.
* Reject approval after run cancellation.
* Publish an event after approval or rejection.

## 8.6 Email simulation task

Locally:

* Send through Mailpit or MailHog.
* Support recipient, subject and template variables.
* Restrict recipient domains through configuration where appropriate.

Production adapter:

* Amazon SES interface.
* Do not require production credentials for local use.
* Record delivery attempts.
* Retry temporary provider errors.
* Redact sensitive template variables.

## 8.7 Report-generation task

Implement a deterministic built-in workload useful for load tests.

Example:

* Accept structured JSON rows.
* Calculate summary statistics.
* Produce a CSV or JSON report.
* Store small reports in development.
* Design an object-storage interface for larger reports.
* Use S3 in AWS deployment.
* Return a signed or authenticated download reference.
* Enforce size and retention limits.

---

# 9. RETRIES, LEASES AND FAILURE RECOVERY

## 9.1 Retry policies

Support:

* No retry
* Fixed delay
* Exponential backoff
* Maximum attempts
* Maximum delay
* Jitter
* Retryable error categories

Example error categories:

* `VALIDATION`
* `AUTHORIZATION`
* `TRANSIENT_NETWORK`
* `RATE_LIMIT`
* `DEPENDENCY_UNAVAILABLE`
* `TIMEOUT`
* `PERMANENT_REMOTE_ERROR`
* `INTERNAL`

Do not retry validation or authorization errors automatically.

Test retry calculations deterministically by injecting a clock and random source.

## 9.2 Worker leases

When a worker claims a task:

1. Atomically transition it to `RUNNING`.
2. Assign a unique lease token.
3. Store worker ID.
4. Store lease expiration.
5. Begin heartbeat updates.
6. Verify the lease before final state transitions.

If the worker crashes:

* Lease eventually expires.
* Scheduler identifies the expired lease.
* Task becomes retryable or failed according to policy.
* A stale worker cannot complete the task using an expired lease.
* Duplicate completion attempts must not overwrite the accepted result.

## 9.3 Cancellation

Cancellation must be state-aware.

* Pending tasks should become cancelled.
* Running tasks receive a cancellation request.
* Interrupt only handlers designed for cooperative interruption.
* Do not incorrectly mark external side effects as reversed.
* Manual approvals become unavailable.
* Final workflow status becomes `CANCELLED` after all active work reaches a safe terminal state.
* Record who requested cancellation and why.

## 9.4 Dead-letter handling

After permanent failure:

* Mark task dead-lettered.
* Write a dead-letter database record.
* Publish to a dead-letter Kafka topic.
* Display the failure in the frontend.
* Allow authorized replay only after validation.
* Ensure replay creates a new attempt and does not erase history.

---

# 10. KAFKA DESIGN

Suggested topics:

```text
taskforge.task.ready.v1
taskforge.task.result.v1
taskforge.workflow.events.v1
taskforge.notification.requested.v1
taskforge.audit.events.v1
taskforge.dead-letter.v1
```

Every event must include:

```text
eventId
eventType
eventVersion
occurredAt
organizationId
correlationId
causationId
producer
payload
```

Requirements:

* Use JSON Schema or Avro-compatible contracts stored under `contracts/events`.
* Version event schemas.
* Validate serialization.
* Configure meaningful partition keys.
* Partition task events by workflow run or task execution where ordering matters.
* Configure consumer groups explicitly.
* Disable unsafe auto-commit behaviour.
* Commit offsets only after successful processing.
* Handle deserialization errors.
* Add dead-letter routing.
* Document retention and partition assumptions.
* Use KRaft mode locally.
* Do not depend on ZooKeeper.
* Include topic initialization for local development.

---

# 11. REDIS USAGE

Use Redis meaningfully for:

* API rate limiting
* Short-lived idempotency-response caching
* Dashboard/read-model caching
* Distributed coordination only where loss is acceptable or backed by PostgreSQL
* Short-lived session/security counters
* Optional server-sent event fan-out between control-plane instances

Do not store the authoritative task state only in Redis.

Requirements:

* Prefix keys by environment and purpose.
* Set explicit TTLs.
* Avoid unbounded key growth.
* Handle Redis outages gracefully.
* Do not fail completed business transactions merely because cache invalidation failed.
* Record cache hit and miss metrics.
* Test fallback behaviour.

---

# 12. BACKEND IMPLEMENTATION

Use package root:

```text
com.mihirgamre.taskforge
```

Use:

* Constructor injection
* Java records for immutable DTOs where appropriate
* Bean Validation
* Spring Security
* Spring Data JPA
* Flyway
* Spring for Apache Kafka
* Spring Data Redis
* Spring Boot Actuator
* Micrometer
* OpenTelemetry
* Testcontainers
* JUnit
* Mockito
* AssertJ

Avoid unnecessary Lombok. Prefer explicit, understandable code.

## 12.1 Layering

Use clear boundaries:

```text
api/
application/
domain/
infrastructure/
security/
configuration/
```

Controllers must not contain business logic.

Repositories must not make authorization decisions.

Domain logic should not depend directly on HTTP.

Avoid returning JPA entities from controllers.

Use explicit request and response DTOs.

## 12.2 API conventions

Base path:

```text
/api/v1
```

Use:

* JSON
* ISO-8601 timestamps
* Cursor pagination for large time-ordered resources
* Stable sorting
* Filtering
* Request IDs
* Correlation IDs
* Idempotency keys
* Optimistic concurrency where relevant
* RFC-style Problem Details error responses
* OpenAPI documentation
* Generated TypeScript API types

Example error body:

```json
{
  "type": "https://taskforge.dev/problems/workflow-validation",
  "title": "Workflow validation failed",
  "status": 422,
  "code": "WORKFLOW_VALIDATION_FAILED",
  "detail": "The workflow contains invalid nodes.",
  "instance": "/api/v1/workflows/...",
  "requestId": "...",
  "errors": [
    {
      "field": "nodes.payment.url",
      "code": "URL_NOT_ALLOWED",
      "message": "The URL is not in the organization allow-list."
    }
  ]
}
```

Do not expose stack traces to clients.

## 12.3 API endpoints

Implement at minimum:

### Authentication

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/logout-all
POST   /api/v1/auth/verify-email
POST   /api/v1/auth/resend-verification
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
GET    /api/v1/auth/me
```

### Organizations

```text
GET    /api/v1/organizations
POST   /api/v1/organizations
GET    /api/v1/organizations/{organizationId}
PATCH  /api/v1/organizations/{organizationId}
GET    /api/v1/organizations/{organizationId}/members
POST   /api/v1/organizations/{organizationId}/invitations
POST   /api/v1/organizations/{organizationId}/invitations/{token}/accept
PATCH  /api/v1/organizations/{organizationId}/members/{memberId}
DELETE /api/v1/organizations/{organizationId}/members/{memberId}
```

### Workflows

```text
GET    /api/v1/organizations/{organizationId}/workflows
POST   /api/v1/organizations/{organizationId}/workflows
GET    /api/v1/workflows/{workflowId}
PATCH  /api/v1/workflows/{workflowId}
DELETE /api/v1/workflows/{workflowId}
GET    /api/v1/workflows/{workflowId}/versions
POST   /api/v1/workflows/{workflowId}/draft
GET    /api/v1/workflows/{workflowId}/draft
PUT    /api/v1/workflows/{workflowId}/draft/graph
POST   /api/v1/workflows/{workflowId}/draft/validate
POST   /api/v1/workflows/{workflowId}/draft/publish
```

### Runs

```text
GET    /api/v1/organizations/{organizationId}/runs
POST   /api/v1/workflows/{workflowId}/runs
GET    /api/v1/runs/{runId}
GET    /api/v1/runs/{runId}/tasks
GET    /api/v1/runs/{runId}/events
GET    /api/v1/runs/{runId}/stream
POST   /api/v1/runs/{runId}/cancel
POST   /api/v1/runs/{runId}/retry
POST   /api/v1/tasks/{taskExecutionId}/retry
POST   /api/v1/tasks/{taskExecutionId}/approve
POST   /api/v1/tasks/{taskExecutionId}/reject
```

Require `Idempotency-Key` for workflow run creation and other side-effecting API operations where replay is likely.

### Schedules

```text
GET    /api/v1/workflows/{workflowId}/schedules
POST   /api/v1/workflows/{workflowId}/schedules
PATCH  /api/v1/schedules/{scheduleId}
DELETE /api/v1/schedules/{scheduleId}
POST   /api/v1/schedules/{scheduleId}/enable
POST   /api/v1/schedules/{scheduleId}/disable
```

### Secrets

```text
GET    /api/v1/organizations/{organizationId}/secrets
POST   /api/v1/organizations/{organizationId}/secrets
PATCH  /api/v1/secrets/{secretId}
DELETE /api/v1/secrets/{secretId}
POST   /api/v1/secrets/{secretId}/rotate
```

Never return the secret value.

### API keys

```text
GET    /api/v1/organizations/{organizationId}/api-keys
POST   /api/v1/organizations/{organizationId}/api-keys
DELETE /api/v1/api-keys/{apiKeyId}
```

### Dashboard and operations

```text
GET    /api/v1/organizations/{organizationId}/dashboard
GET    /api/v1/organizations/{organizationId}/metrics
GET    /api/v1/organizations/{organizationId}/audit-logs
GET    /api/v1/organizations/{organizationId}/dead-letters
POST   /api/v1/dead-letters/{deadLetterId}/replay
```

## 12.4 Pagination

Use cursor pagination for:

* Workflow runs
* Audit logs
* Task attempts
* Dead-letter records

Cursors must be opaque to clients.

Do not use unstable offset pagination for rapidly changing execution history.

---

# 13. AUTHENTICATION AND SECURITY

## 13.1 Authentication design

Use:

* Short-lived signed JWT access tokens
* Opaque rotating refresh tokens
* Refresh token in a `Secure`, `HttpOnly` cookie
* Access token held in application memory, not `localStorage`
* Password hashing through a modern Spring Security password encoder
* Strong random reset and verification tokens
* Token hashing in the database
* Token expiration
* Refresh-token reuse detection
* Account lockout or progressive delay
* Email verification
* Logout and logout-all

For local HTTP development, allow an explicitly documented non-secure cookie mode. Production must require HTTPS.

## 13.2 CSRF

Refresh, logout and other cookie-authenticated endpoints require CSRF protection.

Use:

* SameSite cookies
* Origin validation
* A CSRF token mechanism
* Explicit CORS allow-list

Do not use wildcard production CORS.

## 13.3 Authorization

Every protected action must verify:

* Authenticated principal
* Organization membership
* Required role or permission
* Resource belongs to the same organization
* Resource has not been archived or disabled

Add integration tests attempting cross-tenant access.

A user from Organization A must never read or modify Organization B’s:

* Workflows
* Runs
* Tasks
* Secrets
* API keys
* Audit logs
* Members
* Metrics

## 13.4 Input and output security

Implement:

* Bean validation
* JSON payload-size limits
* String-length limits
* File-size limits
* MIME-type checks
* Safe filename handling
* Output encoding
* Strong DTO allow-lists
* Unknown-field policy
* SQL parameterization
* Safe error messages
* Log redaction
* Request timeouts
* Rate limits
* Security headers

## 13.5 Rate limiting

Use Redis-backed token-bucket or sliding-window limiting.

Apply separate policies to:

* Login
* Registration
* Password reset
* Email verification
* API-key workflow triggers
* Workflow run creation
* Secret operations
* General authenticated API usage

Return appropriate rate-limit headers.

## 13.6 Audit logging

Record security- and business-significant events:

* Login success and failure
* Password changes
* Refresh-token reuse
* Organization creation
* Member invitation
* Role changes
* Workflow creation
* Workflow publication
* Workflow execution
* Cancellation
* Manual approval
* Secret creation or rotation
* API-key creation or revocation
* Dead-letter replay

Audit records must include:

* Actor
* Organization
* Action
* Resource type and ID
* Timestamp
* Correlation ID
* Safe metadata
* Result

Never store secret values in audit records.

## 13.7 Threat model

Create `docs/THREAT_MODEL.md`.

Include at least:

* Cross-tenant data access
* Credential stuffing
* Token theft
* Refresh-token replay
* CSRF
* XSS
* SQL injection
* SSRF through HTTP tasks
* Kafka message forgery
* Poison messages
* Secret leakage
* Log injection
* Denial of service
* Oversized payloads
* Workflow graph explosion
* Duplicate event delivery
* Stale worker completion
* Privilege escalation
* Insecure direct object references

Describe mitigations and remaining risks.

---

# 14. FRONTEND APPLICATION

Build a polished, responsive React and TypeScript application.

Use:

* React 19.x
* TypeScript strict mode
* Vite
* React Router
* TanStack Query for server state
* React Hook Form
* Zod for client-side form validation
* React Flow or a current supported equivalent for the workflow canvas
* Tailwind CSS
* An accessible component system
* Vitest
* React Testing Library
* Playwright

Use a generated or strongly typed API client from the OpenAPI contract.

Do not duplicate backend DTOs manually when generation is practical.

## 14.1 Frontend routes

Suggested routes:

```text
/
 /login
 /register
 /verify-email
 /forgot-password
 /reset-password

/app
/app/onboarding
/app/organizations
/app/:organizationSlug/dashboard
/app/:organizationSlug/workflows
/app/:organizationSlug/workflows/new
/app/:organizationSlug/workflows/:workflowId
/app/:organizationSlug/workflows/:workflowId/builder
/app/:organizationSlug/workflows/:workflowId/versions
/app/:organizationSlug/runs
/app/:organizationSlug/runs/:runId
/app/:organizationSlug/approvals
/app/:organizationSlug/schedules
/app/:organizationSlug/dead-letters
/app/:organizationSlug/team
/app/:organizationSlug/secrets
/app/:organizationSlug/api-keys
/app/:organizationSlug/audit
/app/:organizationSlug/settings
```

## 14.2 Main layout

Include:

* Collapsible sidebar
* Organization selector
* Breadcrumbs
* Global search where useful
* User menu
* Responsive mobile navigation
* Clear environment indicator outside production
* Notification area
* Connection or live-update status

## 14.3 Dashboard

Display:

* Runs in the last 24 hours
* Success rate
* Failure rate
* Currently running workflows
* Waiting approvals
* Dead-letter count
* Median and p95 run duration
* Task throughput
* Recent runs
* Most failure-prone workflows
* Retry activity

Charts must have:

* Accessible labels
* Textual summaries
* Tooltips
* Empty states
* Loading states
* Failure states
* Time-range controls

Do not invent analytics values. Load from backend endpoints.

## 14.4 Workflow list

Support:

* Search
* Status filter
* Sorting
* Pagination
* Draft/published indicators
* Last-run result
* Last updated
* Quick actions
* Archive confirmation
* Empty-state onboarding

## 14.5 Visual workflow builder

The builder is a central portfolio feature.

Include:

* Node palette
* Drag-and-drop node creation
* Connectable handles
* Pan and zoom
* Keyboard navigation
* Node selection
* Configuration panel
* Delete confirmation
* Undo and redo
* Autosave with visible save state
* Validation panel
* Publish workflow
* Run workflow
* Fit-to-view
* Mini-map if accessible
* Read-only view for published versions
* Unsaved-change protection
* Optimistic concurrency conflict handling

Node appearance must show:

* Task type
* Name
* Validation status
* Retry count
* Timeout
* Secret usage indicator
* Configuration completeness

Before publish:

* Call backend validation.
* Show all validation failures.
* Clicking an error should focus the relevant node.
* Prevent publication with blocking errors.

Do not rely solely on frontend graph validation.

## 14.6 Run details page

Include:

* Overall status
* Workflow version
* Trigger
* Requester
* Start and finish times
* Duration
* Input
* Output
* Graph with node execution status
* Task timeline
* Attempt history
* Retry information
* Error details
* Correlation and trace IDs
* Cancel action
* Retry action
* Manual-approval controls
* Live updates through server-sent events
* Polling fallback when SSE is unavailable

Use status labels and icons, not colour alone.

## 14.7 Manual approvals

Create an approvals inbox.

Display:

* Workflow
* Task
* Requester
* Created time
* Expiration
* Approval context
* Approve button
* Reject button
* Comment field

Require confirmation before rejection when rejection fails the workflow.

## 14.8 Secrets interface

Support:

* List secret names and metadata
* Create secret
* Rotate secret
* Delete secret with dependency warning
* Never reveal stored values
* Clearly state that values cannot be retrieved after saving
* Validate secret names
* Display which workflows reference a secret without revealing its value

## 14.9 API keys interface

Support:

* Create named key
* Select scopes
* Set expiration
* Show complete key exactly once
* Copy warning and confirmation
* List prefixes, scopes, creation and last-use dates
* Revoke key

## 14.10 Frontend resilience

Implement:

* Global error boundary
* Route-level error states
* Network timeout handling
* Authentication-expiry handling
* Refresh-token coordination
* Request cancellation
* Duplicate-submission protection
* Loading skeletons
* Empty states
* Toasts for transient feedback
* Persistent error notices for important failures
* Form field errors
* Optimistic updates only when safe
* Accessibility-focused keyboard and focus management

## 14.11 Accessibility

Target WCAG 2.2 AA.

At minimum:

* Semantic HTML
* Visible focus states
* Keyboard-operable workflow builder
* Proper labels
* ARIA only where needed
* Sufficient contrast
* No colour-only information
* Skip navigation
* Dialog focus trapping
* Focus restoration
* Screen-reader status announcements
* Reduced-motion support

Run automated accessibility checks and document manual checks.

---

# 15. SERVER-SENT EVENTS

Use server-sent events for live run status.

Requirements:

* Authenticated stream
* Organization authorization
* Heartbeat messages
* Event IDs
* Reconnection support
* Last-Event-ID handling where practical
* Per-user and per-organization connection limits
* Safe cleanup after disconnect
* Metrics for active connections
* Polling fallback
* Redis-based fan-out if multiple control-plane instances are deployed

Do not require WebSockets unless a concrete need emerges.

---

# 16. DATABASE CORRECTNESS

## 16.1 Transactions

Define transaction boundaries deliberately.

Examples:

* User and default organization onboarding
* Workflow version publication
* Run creation and root-task initialization
* Task state transition and outbox insertion
* Approval state transition
* Scheduler task claim
* Worker completion
* Retry scheduling
* API-key creation
* Refresh-token rotation

Avoid network calls inside long database transactions.

## 16.2 Concurrency

Use a combination of:

* Optimistic locking
* Unique constraints
* Compare-and-set updates
* Row locks
* `SKIP LOCKED`
* Idempotency keys

Create tests for:

* Two requests publishing the same draft
* Multiple schedulers firing one schedule
* Multiple workers claiming one task
* Duplicate Kafka delivery
* Simultaneous approvals
* Cancellation racing with completion
* Stale worker lease completion
* Refresh-token reuse
* Duplicate API-key trigger
* Workflow retry racing with manual retry

## 16.3 Migration safety

Flyway migrations must:

* Be ordered
* Be immutable after merging
* Include indexes
* Avoid destructive changes without a migration strategy
* Be tested against an existing previous schema
* Include down-migration documentation even if Flyway uses forward-only migrations

---

# 17. TESTING STRATEGY

Create `docs/TESTING.md`.

## 17.1 Unit tests

Test:

* Workflow DAG cycle detection
* Root-node discovery
* Dependency evaluation
* Conditional logic
* Workflow state transitions
* Task state transitions
* Retry backoff
* Jitter bounds
* Lease expiration
* Idempotency
* Secret-reference parsing
* Schedule calculations
* DST edge cases
* Permission matrix
* Error categorization
* HTTP destination validation
* JSON transformations
* Output-size limiting

## 17.2 Spring slice tests

Use appropriate slices for:

* Controllers
* Security
* JPA repositories
* JSON serialization

## 17.3 Integration tests

Use Testcontainers for:

* PostgreSQL
* Kafka
* Redis
* Mailpit/MailHog where useful

Integration tests must verify:

* Flyway migrations
* Outbox publication
* Kafka consumption
* Consumer idempotency
* Worker processing
* Retry scheduling
* Dead-letter routing
* Redis outage fallback
* Cross-tenant authorization
* Refresh-token rotation
* Scheduled workflow execution
* Manual approval
* Run cancellation
* SSE authorization

Do not replace Kafka integration tests entirely with mocks.

## 17.4 End-to-end tests

Use Playwright.

Critical journeys:

1. Register and verify account.
2. Create organization.
3. Create workflow.
4. Add and connect nodes.
5. Fix validation errors.
6. Publish workflow.
7. Trigger workflow.
8. Observe live execution.
9. Approve manual step.
10. View successful completion.
11. Create a scheduled workflow.
12. Create and use an API key.
13. Trigger a failure and retry.
14. View a dead-letter record.
15. Confirm viewer cannot edit.
16. Confirm cross-tenant URLs are rejected.
17. Confirm refresh after access-token expiry.
18. Confirm logout revokes session.

## 17.5 Security tests

Test:

* Invalid JWT
* Expired JWT
* Revoked refresh token
* Reused refresh token
* CSRF attempt
* CORS rejection
* Brute-force rate limit
* SQL-injection payloads
* XSS payload rendering
* SSRF attempts
* Oversized payload
* Path traversal filenames
* Cross-tenant resource access
* Role escalation
* Secret leakage in responses
* Secret leakage in logs
* API-key scope enforcement

## 17.6 Concurrency and reliability tests

Create automated scenarios that:

* Submit thousands of idempotent duplicate requests.
* Start multiple schedulers.
* Start multiple workers.
* Kill a worker during execution.
* Allow its lease to expire.
* Verify another worker safely retries.
* Deliver the same Kafka event repeatedly.
* Verify only one business outcome.
* Simulate Kafka unavailability.
* Simulate Redis unavailability.
* Simulate temporary HTTP dependency failure.
* Simulate database connection interruption where feasible.

## 17.7 Coverage

Use coverage as a guardrail, not as the only quality metric.

Targets:

* At least 80% line coverage for domain and application logic.
* Strong branch coverage for state machines and graph validation.
* No strict high target for generated code or configuration.
* Publish coverage reports in CI.

---

# 18. OBSERVABILITY

Use:

* Spring Boot Actuator
* Micrometer
* OpenTelemetry
* Prometheus
* Grafana
* Structured JSON logs
* Trace and correlation IDs
* Health and readiness probes

OpenTelemetry supports traces, metrics and logs for Java applications, while Prometheus and Grafana can provide metrics collection, visualization and alerting. Use these tools as real operational components rather than decorative containers.

## 18.1 Metrics

Expose meaningful metrics such as:

```text
taskforge_workflow_runs_started_total
taskforge_workflow_runs_completed_total
taskforge_workflow_runs_failed_total
taskforge_workflow_run_duration_seconds
taskforge_tasks_dispatched_total
taskforge_tasks_completed_total
taskforge_tasks_failed_total
taskforge_task_duration_seconds
taskforge_task_retries_total
taskforge_dead_letters_total
taskforge_scheduler_lag_seconds
taskforge_ready_queue_depth
taskforge_running_tasks
taskforge_worker_heartbeats
taskforge_expired_leases_total
taskforge_outbox_pending
taskforge_outbox_publish_failures_total
taskforge_kafka_consumer_lag
taskforge_cache_hits_total
taskforge_cache_misses_total
taskforge_sse_connections
taskforge_rate_limit_rejections_total
```

Avoid high-cardinality labels such as user ID, workflow-run ID or task ID.

## 18.2 Logs

Use structured fields:

* timestamp
* level
* service
* environment
* requestId
* correlationId
* traceId
* organizationId where safe
* workflowRunId where safe
* taskExecutionId where safe
* eventType
* errorCode

Never log:

* Passwords
* Refresh tokens
* Access tokens
* Complete API keys
* Secret values
* Authorization headers
* Unredacted task payloads by default

## 18.3 Tracing

Trace:

* Incoming API request
* Run creation
* Outbox publication
* Kafka event processing
* Task execution
* Downstream HTTP request
* Result publication
* Workflow completion

Propagate correlation and trace context through Kafka headers.

## 18.4 Grafana dashboards

Provision dashboards automatically for:

1. Platform overview
2. Workflow execution
3. Worker health
4. Scheduler health
5. Kafka and outbox
6. API performance
7. Error and retry analysis

Add useful local alerts:

* Growing dead-letter count
* Excessive scheduler lag
* No worker heartbeats
* High task failure rate
* Large outbox backlog
* High API error rate
* Database connection exhaustion

---

# 19. LOCAL DEVELOPMENT

Create a single local startup experience.

Expected command:

```bash
docker compose up --build
```

Compose should include:

* PostgreSQL
* Redis
* Kafka in KRaft mode
* Kafka initialization
* Mailpit or MailHog
* Mock HTTP dependency service
* OpenTelemetry Collector
* Prometheus
* Grafana
* Control plane
* Scheduler
* At least two worker instances
* Notification worker
* Frontend

Requirements:

* Health checks
* Dependency readiness
* Named volumes
* Isolated networks
* Environment-variable configuration
* No real secrets
* Seeded development data
* A documented demo account
* Development-only account seeding disabled in production
* Idempotent initialization
* Useful startup errors

Provide separate development commands for faster iteration:

```bash
./mvnw verify
./mvnw spring-boot:run
npm ci
npm run dev
npm run lint
npm run typecheck
npm test
npm run e2e
```

Add a Makefile or scripts for:

```text
make setup
make up
make down
make reset
make test
make test-backend
make test-frontend
make test-integration
make e2e
make load-test
make lint
make format
```

Ensure Windows users have documented PowerShell equivalents where appropriate.

---

# 20. CONTAINERIZATION

Create multi-stage Dockerfiles.

Requirements:

* Small runtime image
* Non-root user
* No build tools in runtime image
* Health checks
* Explicit JVM options
* Graceful shutdown
* Read-only filesystem where practical
* Temporary writable directory only where needed
* No secrets baked into images
* Image labels
* Pinned base-image versions or digests where practical
* `.dockerignore`
* Reproducible builds

Generate an SBOM in CI.

Scan images for vulnerabilities.

---

# 21. CI/CD

Create GitHub Actions workflows.

## 21.1 Pull-request workflow

Run:

* Backend formatting check
* Static analysis
* Compilation
* Unit tests
* Integration tests
* Flyway migration tests
* Frontend lint
* Frontend formatting check
* TypeScript type checking
* Frontend unit tests
* Production frontend build
* Playwright tests where feasible
* OpenAPI compatibility check
* Event-schema compatibility check
* Docker build
* Dependency vulnerability scanning
* Secret scanning
* Container vulnerability scanning
* Test and coverage reports

## 21.2 Main-branch workflow

After all checks pass:

* Build versioned container images.
* Tag with commit SHA.
* Generate SBOM.
* Push to Amazon ECR when AWS deployment is enabled.
* Apply infrastructure changes only through protected environments.
* Deploy services.
* Run database migrations safely.
* Run smoke tests.
* Roll back or halt on failed health checks.

## 21.3 Dependency maintenance

Configure Dependabot or equivalent for:

* Maven
* npm
* GitHub Actions
* Docker

Do not automatically merge major upgrades.

---

# 22. AWS DEPLOYMENT

Use Terraform.

Production-oriented architecture:

* Route 53 for DNS
* ACM for TLS certificates
* CloudFront and S3 for frontend assets
* Application Load Balancer
* ECS Fargate for control plane, scheduler, workers and notification worker
* Amazon ECR
* RDS PostgreSQL
* ElastiCache for Redis
* Amazon MSK or MSK Serverless for Kafka
* S3 for generated reports
* Amazon SES adapter for emails
* Secrets Manager
* KMS
* CloudWatch logs
* OpenTelemetry export
* AWS WAF where appropriate
* VPC with public and private subnets
* NAT strategy documented carefully
* Security groups with least privilege
* IAM task roles
* Automated backups
* RDS deletion protection in production
* Environment separation

AWS Fargate should be used to run containers without managing EC2 cluster hosts.

## 22.1 Cost-conscious demo environment

Also create a documented demo profile.

The demo profile may reduce redundancy but must clearly state that it is not highly available.

Include:

* Small task counts
* Autoscaling minimums
* Short log retention
* Optional managed-service toggles
* AWS Budget alarm
* Cost tags
* `terraform destroy` instructions
* Clear warning about resources that continue charging

Never create cloud resources automatically without an explicit deployment command.

Never commit AWS credentials.

## 22.2 Terraform quality

Terraform must include:

* Reusable modules
* Environment-specific variables
* Remote-state guidance
* Sensitive outputs
* Formatting and validation
* Static analysis
* Least-privilege IAM
* Resource tagging
* Documented prerequisites
* No hard-coded account IDs
* No hard-coded credentials

---

# 23. PERFORMANCE AND LOAD TESTING

Use k6.

Create reproducible scenarios for:

1. Login and dashboard reads
2. Workflow run submission
3. Idempotent duplicate submission
4. High-volume task dispatch
5. Multiple worker scaling
6. Manual approval
7. SSE connection load
8. Scheduled-run bursts
9. Downstream HTTP latency
10. Downstream HTTP failure and retry

Record:

* Test environment
* CPU
* Memory
* Worker count
* Database configuration
* Kafka partitions
* Virtual users
* Test duration
* Request rate
* p50 latency
* p95 latency
* p99 latency
* Error rate
* Jobs per second
* Scheduler lag
* Duplicate business effects
* Retry count
* Dead-letter count

Initial quality targets:

* Zero cross-tenant data leaks.
* Zero duplicate task side effects in idempotency tests.
* Zero workflow runs created from repeated requests using one idempotency key.
* No task claimed successfully by two active leases.
* API p95 under 300 ms for ordinary authenticated read operations in the documented local test environment.
* Run-submission p95 under 500 ms at moderate load.
* Scheduler lag p95 under 2 seconds under the documented target workload.
* No negative or impossible state transitions.
* Graceful recovery after a worker is terminated.
* No lost durable workflow state after Redis restart.
* Successful continuation after duplicate Kafka delivery.

Treat these as engineering targets, not fabricated claims.

If a target is missed:

1. Report the actual result.
2. Investigate.
3. Optimize.
4. Re-run.
5. Document both before and after results.

Create `docs/LOAD_TEST_REPORT.md`.

---

# 24. DOCUMENTATION

The README must be recruiter-friendly and engineer-friendly.

Include:

* Project overview
* Problem being solved
* Key capabilities
* Architecture diagram
* Technology choices
* Repository structure
* Local setup
* Demo credentials
* Screenshots or GIFs
* Example workflow
* API documentation
* Testing commands
* Load-test summary
* Security considerations
* Deployment architecture
* Trade-offs
* Known limitations
* Future improvements
* Résumé-ready metrics only after measurement

Use Mermaid diagrams for:

1. System architecture
2. Database relationships
3. Workflow-run sequence
4. Transactional outbox
5. Worker lease recovery
6. Authentication refresh flow
7. AWS deployment

Create architecture decision records for major choices, including:

* PostgreSQL as source of truth
* At-least-once processing
* Transactional outbox
* Kafka rather than synchronous service calls for task dispatch
* Redis as non-authoritative infrastructure
* SSE rather than WebSockets
* Modular services rather than many microservices
* ECS Fargate rather than Kubernetes
* Restricted built-in tasks rather than arbitrary code execution
* Workflow-version immutability
* Refresh-token rotation

---

# 25. DEMO DATA

Create deterministic seed data in development only.

Include:

* Demo owner
* Demo member
* Demo viewer
* One organization
* A successful workflow
* A workflow requiring manual approval
* A workflow with a transient failure and retry
* A workflow that dead-letters
* Recent workflow runs
* Audit-log examples
* One disabled schedule
* One enabled recurring schedule
* Mock secrets without real credentials

Do not seed production.

---

# 26. USER EXPERIENCE QUALITY

Treat all common states as first-class:

* Loading
* Empty
* Success
* Validation failure
* Authorization failure
* Network failure
* Service unavailable
* Partial data
* Expired session
* Conflict from concurrent editing
* Deleted or archived resource
* No worker available
* Workflow paused for approval
* Run cancellation pending
* Dead-lettered task

Use clear, nontechnical error messages for users and preserve technical details for authorized debugging views.

Examples:

Bad:

```text
NullPointerException
```

Good:

```text
The workflow could not start because one of its tasks references a secret that no longer exists.
```

Include request and correlation IDs for support.

---

# 27. DEFINITION OF DONE

TaskForge is not complete until:

* A user can register and sign in.
* Authentication is secure and tested.
* Multi-tenant organization authorization works.
* A user can create a visual workflow.
* The workflow can be validated.
* A workflow version can be published.
* Published versions are immutable.
* A workflow can be run manually.
* A workflow can be scheduled.
* An API key can trigger a workflow.
* Workers process tasks concurrently.
* Retries work.
* Worker lease recovery works.
* Duplicate events do not create duplicate business effects.
* Manual approval works.
* Cancellation works.
* Dead-letter handling works.
* The frontend displays live progress.
* Secrets remain hidden.
* Audit history works.
* Docker Compose starts the complete local stack.
* Unit, integration and end-to-end tests pass.
* Load tests exist and produce documented results.
* Grafana dashboards work.
* CI passes.
* Terraform validates.
* Deployment documentation is complete.
* README accurately reflects implemented functionality.
* No fake metrics or unsupported résumé claims appear.

---

# 28. IMPLEMENTATION PHASES

## Phase 0: Foundation and design

Deliver:

* Repository structure
* Dependency decisions
* Architecture document
* ADRs
* ERD
* Event definitions
* API skeleton
* Threat model
* Roadmap
* Local Compose infrastructure
* Build verification

Acceptance criteria:

* Backend and frontend build.
* PostgreSQL, Redis and Kafka start locally.
* Health checks work.
* CI skeleton runs.
* Documentation matches the design.

## Phase 1: Vertical execution slice

Build the smallest complete production-style flow:

1. Create one development user.
2. Create one organization.
3. Create a one-node workflow.
4. Publish it.
5. Trigger a run.
6. Insert root task and outbox record.
7. Publish Kafka event.
8. Worker claims task.
9. Worker executes deterministic report task.
10. Worker records success.
11. Workflow run becomes successful.
12. Frontend displays the result.

Do not begin advanced features until this vertical slice works with integration tests.

## Phase 2: Authentication and tenancy

Deliver:

* Registration
* Login
* JWT access tokens
* Refresh rotation
* Email verification
* Password reset
* Organizations
* Membership roles
* Cross-tenant tests
* Audit logging
* Rate limiting

## Phase 3: Workflow builder and versioning

Deliver:

* Workflow CRUD
* Visual builder
* Draft autosave
* DAG validation
* Immutable publishing
* Version history
* Optimistic editing conflicts
* Builder tests

## Phase 4: Scheduler and distributed workers

Deliver:

* Task dispatch
* Multiple workers
* Worker leases
* Heartbeats
* Retry policies
* Expired-lease recovery
* At-least-once idempotency
* Dead-letter handling
* Concurrency tests

## Phase 5: Task handlers

Deliver:

* Delay
* JSON transform
* Conditional
* Manual approval
* Email simulation
* Report generation
* Restricted HTTP request
* Handler-specific security and tests

## Phase 6: Scheduling and API triggers

Deliver:

* One-time schedules
* Cron schedules
* Time zones
* DST tests
* API keys
* Scopes
* Trigger idempotency
* Schedule management UI

## Phase 7: Full operational frontend

Deliver:

* Dashboard
* Run list
* Run detail graph
* Live SSE updates
* Approvals inbox
* Secrets
* API keys
* Audit logs
* Dead-letter interface
* Responsive and accessible UI

## Phase 8: Observability and resilience

Deliver:

* OpenTelemetry
* Prometheus
* Grafana
* Structured logs
* Alert rules
* Redis fallback
* Kafka failure handling
* Worker failure tests
* Reconciliation jobs

## Phase 9: Security hardening

Deliver:

* Threat-model review
* SSRF protections
* CSRF protections
* CORS
* Security headers
* Secret encryption
* API rate limits
* Log-redaction tests
* Dependency and container scans

## Phase 10: Performance

Deliver:

* k6 suite
* One-, two-, four- and eight-worker comparison
* Performance bottleneck analysis
* Query/index analysis
* Before-and-after optimizations
* Load-test report
* Honest measurable results

## Phase 11: AWS and CI/CD

Deliver:

* Terraform
* AWS architecture
* ECR
* ECS Fargate
* RDS
* Redis
* Kafka
* S3
* Secrets
* Cloud deployment workflow
* Smoke tests
* Cost controls
* Deployment guide

## Phase 12: Portfolio polish

Deliver:

* Final README
* Screenshots
* Demo video plan
* Architecture diagrams
* Final test report
* Final performance report
* Known limitations
* Future roadmap
* Concise résumé bullet suggestions based only on measured results

---

# 29. REQUIRED CHECKPOINT FORMAT

At the end of each phase, report:

```text
Phase completed:
Implemented:
Files changed:
Architecture decisions:
Tests run:
Test results:
Manual verification:
Security considerations:
Known limitations:
Next phase:
```

Include exact commands that were executed.

Do not say “all tests pass” unless the output confirms that.

---

# 30. INITIAL ACTION

Begin now.

1. Inspect the repository.
2. Create the project structure if it does not exist.
3. Write the architecture and roadmap documents.
4. Select and pin compatible dependencies.
5. Build Phase 0.
6. Build the Phase 1 vertical execution slice.
7. Run the tests and local stack.
8. Fix failures.
9. Report verified results using the required checkpoint format.
10. Continue into later phases if the environment and context permit.

Prioritize correctness, clear architecture and a working vertical slice over producing a large quantity of unverified code.
