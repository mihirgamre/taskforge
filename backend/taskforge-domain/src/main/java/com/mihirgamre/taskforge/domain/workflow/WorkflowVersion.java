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
@Table(name = "workflow_version")
public class WorkflowVersion {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowVersionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected WorkflowVersion() {
    }

    private WorkflowVersion(UUID id, UUID workflowId, int versionNumber, Instant now) {
        this.id = id;
        this.workflowId = workflowId;
        this.versionNumber = versionNumber;
        this.status = WorkflowVersionStatus.DRAFT;
        this.createdAt = now;
    }

    public static WorkflowVersion draft(UUID workflowId, int versionNumber, Instant now) {
        return new WorkflowVersion(UUID.randomUUID(), workflowId, versionNumber, now);
    }

    public UUID id() {
        return id;
    }

    public UUID workflowId() {
        return workflowId;
    }

    public int versionNumber() {
        return versionNumber;
    }

    public WorkflowVersionStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public void publish(Instant now) {
        if (status != WorkflowVersionStatus.DRAFT) {
            throw new IllegalStateException("Only draft workflow versions can be published");
        }
        this.status = WorkflowVersionStatus.PUBLISHED;
        this.publishedAt = now;
    }

    public void requireDraft() {
        if (status != WorkflowVersionStatus.DRAFT) {
            throw new IllegalStateException("Published workflow versions are immutable");
        }
    }
}
