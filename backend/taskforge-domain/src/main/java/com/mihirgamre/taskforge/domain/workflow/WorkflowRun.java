package com.mihirgamre.taskforge.domain.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_run")
public class WorkflowRun {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowRunStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_message")
    private String failureMessage;

    protected WorkflowRun() {
    }

    private WorkflowRun(UUID workflowId, UUID workflowVersionId, Instant now) {
        this.id = UUID.randomUUID();
        this.workflowId = workflowId;
        this.workflowVersionId = workflowVersionId;
        this.status = WorkflowRunStatus.RUNNING;
        this.createdAt = now;
        this.startedAt = now;
    }

    public static WorkflowRun start(UUID workflowId, UUID workflowVersionId, Instant now) {
        return new WorkflowRun(workflowId, workflowVersionId, now);
    }

    public UUID id() {
        return id;
    }

    public UUID workflowId() {
        return workflowId;
    }

    public UUID workflowVersionId() {
        return workflowVersionId;
    }

    public WorkflowRunStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String failureMessage() {
        return failureMessage;
    }

    public void markSucceeded(Instant now) {
        if (status == WorkflowRunStatus.RUNNING) {
            this.status = WorkflowRunStatus.SUCCEEDED;
            this.completedAt = now;
        }
    }

    public void markFailed(String failureMessage, Instant now) {
        if (status == WorkflowRunStatus.RUNNING) {
            this.status = WorkflowRunStatus.FAILED;
            this.failureMessage = failureMessage;
            this.completedAt = now;
        }
    }
}
