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
        this.updatedAt = now;
    }

    public void markReady(Instant now) {
        requireStatus(TaskStatus.BLOCKED, "make ready");
        this.status = TaskStatus.PENDING;
        this.updatedAt = now;
    }

    public void markSucceeded(Instant now) {
        requireStatus(TaskStatus.DISPATCHED, "succeed");
        this.status = TaskStatus.SUCCEEDED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(Instant now) {
        requireStatus(TaskStatus.DISPATCHED, "fail");
        this.status = TaskStatus.FAILED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    private void requireStatus(TaskStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException("Cannot " + action + " task " + id + " from status " + status);
        }
    }
}
