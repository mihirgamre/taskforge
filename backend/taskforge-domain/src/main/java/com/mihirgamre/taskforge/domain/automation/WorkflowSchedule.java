package com.mihirgamre.taskforge.domain.automation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_schedule")
public class WorkflowSchedule {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false)
    private String name;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    protected WorkflowSchedule() {
    }

    public WorkflowSchedule(
            UUID organizationId,
            UUID workflowId,
            String name,
            String cronExpression,
            String timeZone,
            Instant nextRunAt,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.workflowId = workflowId;
        this.name = name;
        this.cronExpression = cronExpression;
        this.timeZone = timeZone;
        this.enabled = true;
        this.nextRunAt = nextRunAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID workflowId() {
        return workflowId;
    }

    public String name() {
        return name;
    }

    public String cronExpression() {
        return cronExpression;
    }

    public String timeZone() {
        return timeZone;
    }

    public boolean enabled() {
        return enabled;
    }

    public Instant nextRunAt() {
        return nextRunAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant lastRunAt() {
        return lastRunAt;
    }

    public void disable(Instant now) {
        this.enabled = false;
        this.updatedAt = now;
    }

    public void recordRun(Instant now, Instant nextRunAt) {
        this.lastRunAt = now;
        this.nextRunAt = nextRunAt;
        this.updatedAt = now;
    }
}
