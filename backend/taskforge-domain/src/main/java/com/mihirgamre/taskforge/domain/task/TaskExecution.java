package com.mihirgamre.taskforge.domain.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_execution")
public class TaskExecution {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column
    private String description;

    @Column(name = "workflow_run_id")
    private UUID workflowRunId;

    @Column(name = "workflow_node_key")
    private String workflowNodeKey;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "lease_owner")
    private String leaseOwner;

    @Column(name = "lease_token")
    private UUID leaseToken;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "lease_heartbeat_at")
    private Instant leaseHeartbeatAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "failure_message")
    private String failureMessage;

    protected TaskExecution() {
    }

    private TaskExecution(UUID id, String tenantId, String description, Instant now) {
        this.id = id;
        this.tenantId = tenantId;
        this.taskType = TaskType.NO_OP;
        this.status = TaskStatus.PENDING;
        this.description = description;
        this.createdAt = now;
        this.updatedAt = now;
        this.nextAttemptAt = now;
        this.maxAttempts = 3;
    }

    private TaskExecution(
            UUID id,
            String tenantId,
            String description,
            UUID workflowRunId,
            String workflowNodeKey,
            TaskStatus status,
            Instant now
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.taskType = TaskType.NO_OP;
        this.status = status;
        this.description = description;
        this.workflowRunId = workflowRunId;
        this.workflowNodeKey = workflowNodeKey;
        this.createdAt = now;
        this.updatedAt = now;
        this.nextAttemptAt = now;
        this.maxAttempts = 3;
    }

    public static TaskExecution createNoOp(String tenantId, String description, Instant now) {
        return new TaskExecution(UUID.randomUUID(), tenantId, description, now);
    }

    public static TaskExecution createWorkflowNoOp(
            UUID workflowRunId,
            String workflowNodeKey,
            String description,
            TaskStatus status,
            Instant now
    ) {
        if (status != TaskStatus.PENDING && status != TaskStatus.BLOCKED) {
            throw new IllegalArgumentException("Workflow task must start pending or blocked");
        }
        return new TaskExecution(
                UUID.randomUUID(),
                "workflow",
                description,
                workflowRunId,
                workflowNodeKey,
                status,
                now
        );
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public TaskType taskType() {
        return taskType;
    }

    public TaskStatus status() {
        return status;
    }

    public String description() {
        return description;
    }

    public UUID workflowRunId() {
        return workflowRunId;
    }

    public String workflowNodeKey() {
        return workflowNodeKey;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant dispatchedAt() {
        return dispatchedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String leaseOwner() {
        return leaseOwner;
    }

    public UUID leaseToken() {
        return leaseToken;
    }

    public Instant leaseExpiresAt() {
        return leaseExpiresAt;
    }

    public Instant leaseHeartbeatAt() {
        return leaseHeartbeatAt;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public String failureMessage() {
        return failureMessage;
    }

    public void markDispatched(Instant now) {
        requireStatus(TaskStatus.PENDING, "dispatch");
        this.status = TaskStatus.DISPATCHED;
        this.attemptCount++;
        this.dispatchedAt = now;
        this.updatedAt = now;
    }

    public void markPending(Instant now) {
        requireStatus(TaskStatus.DISPATCHED, "return to pending");
        this.status = TaskStatus.PENDING;
        clearLease();
        this.updatedAt = now;
    }

    public void markReady(Instant now) {
        requireStatus(TaskStatus.BLOCKED, "make ready");
        this.status = TaskStatus.PENDING;
        this.nextAttemptAt = now;
        this.updatedAt = now;
    }

    public void markSucceeded(Instant now) {
        requireStatus(TaskStatus.DISPATCHED, "succeed");
        this.status = TaskStatus.SUCCEEDED;
        this.completedAt = now;
        clearLease();
        this.updatedAt = now;
    }

    public void markFailed(String message, Instant now) {
        requireStatus(TaskStatus.DISPATCHED, "fail");
        this.status = TaskStatus.FAILED;
        this.failureMessage = message;
        this.completedAt = now;
        clearLease();
        this.updatedAt = now;
    }

    public boolean acquireLease(String owner, UUID token, Instant now, Instant expiresAt) {
        requireStatus(TaskStatus.DISPATCHED, "lease");
        if (leaseToken != null && leaseExpiresAt != null && leaseExpiresAt.isAfter(now)) {
            return false;
        }
        this.leaseOwner = owner;
        this.leaseToken = token;
        this.leaseExpiresAt = expiresAt;
        this.leaseHeartbeatAt = now;
        this.updatedAt = now;
        return true;
    }

    public boolean heartbeat(UUID token, Instant now, Instant expiresAt) {
        if (!hasLeaseToken(token) || status != TaskStatus.DISPATCHED) {
            return false;
        }
        this.leaseHeartbeatAt = now;
        this.leaseExpiresAt = expiresAt;
        this.updatedAt = now;
        return true;
    }

    public boolean hasLeaseToken(UUID token) {
        return leaseToken != null && leaseToken.equals(token);
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts;
    }

    public void markRetryableFailure(String message, Instant now, Instant nextAttemptAt) {
        requireStatus(TaskStatus.DISPATCHED, "retry task");
        this.status = TaskStatus.PENDING;
        this.failureMessage = message;
        this.nextAttemptAt = nextAttemptAt;
        clearLease();
        this.updatedAt = now;
    }

    public void markDeadLettered(String message, Instant now) {
        requireStatus(TaskStatus.DISPATCHED, "dead-letter task");
        this.status = TaskStatus.FAILED;
        this.failureMessage = message;
        this.completedAt = now;
        clearLease();
        this.updatedAt = now;
    }

    private void requireStatus(TaskStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException("Cannot " + action + " task " + id + " from status " + status);
        }
    }

    private void clearLease() {
        this.leaseOwner = null;
        this.leaseToken = null;
        this.leaseExpiresAt = null;
        this.leaseHeartbeatAt = null;
    }
}
