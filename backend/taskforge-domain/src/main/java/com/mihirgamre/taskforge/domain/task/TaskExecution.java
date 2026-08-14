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

    public static TaskExecution createNoOp(String tenantId, String description, Instant now) {
        return new TaskExecution(UUID.randomUUID(), tenantId, description, now);
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

    public void markSucceeded(Instant now) {
        requireStatus(TaskStatus.DISPATCHED, "succeed");
        this.status = TaskStatus.SUCCEEDED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    private void requireStatus(TaskStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException("Cannot " + action + " task " + id + " from status " + status);
        }
    }
}
