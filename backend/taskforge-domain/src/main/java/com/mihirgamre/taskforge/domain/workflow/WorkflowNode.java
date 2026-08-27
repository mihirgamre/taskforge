package com.mihirgamre.taskforge.domain.workflow;

import com.mihirgamre.taskforge.domain.task.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_node")
public class WorkflowNode {
    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String configuration;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowNode() {
    }

    public WorkflowNode(UUID workflowVersionId, String nodeKey, TaskType type, String name, String configuration, Instant now) {
        this.id = UUID.randomUUID();
        this.workflowVersionId = workflowVersionId;
        this.nodeKey = nodeKey;
        this.type = type;
        this.name = name;
        this.configuration = configuration == null ? "{}" : configuration;
        this.createdAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID workflowVersionId() {
        return workflowVersionId;
    }

    public String nodeKey() {
        return nodeKey;
    }

    public TaskType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public String configuration() {
        return configuration;
    }
}
