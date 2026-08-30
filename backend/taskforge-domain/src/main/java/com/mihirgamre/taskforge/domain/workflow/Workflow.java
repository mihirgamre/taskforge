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
@Table(name = "workflow")
public class Workflow {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Workflow() {
    }

    private Workflow(UUID id, UUID organizationId, String name, String description, Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
        this.status = WorkflowStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Workflow create(String name, String description, Instant now) {
        return new Workflow(UUID.randomUUID(), null, name, description, now);
    }

    public static Workflow create(UUID organizationId, String name, String description, Instant now) {
        return new Workflow(UUID.randomUUID(), organizationId, name, description, now);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String description() {
        return description;
    }

    public WorkflowStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
