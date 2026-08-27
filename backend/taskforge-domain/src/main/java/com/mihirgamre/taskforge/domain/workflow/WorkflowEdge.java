package com.mihirgamre.taskforge.domain.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "workflow_edge")
public class WorkflowEdge {
    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "source_node_key", nullable = false)
    private String sourceNodeKey;

    @Column(name = "target_node_key", nullable = false)
    private String targetNodeKey;

    protected WorkflowEdge() {
    }

    public WorkflowEdge(UUID workflowVersionId, String sourceNodeKey, String targetNodeKey) {
        this.id = UUID.randomUUID();
        this.workflowVersionId = workflowVersionId;
        this.sourceNodeKey = sourceNodeKey;
        this.targetNodeKey = targetNodeKey;
    }

    public UUID id() {
        return id;
    }

    public UUID workflowVersionId() {
        return workflowVersionId;
    }

    public String sourceNodeKey() {
        return sourceNodeKey;
    }

    public String targetNodeKey() {
        return targetNodeKey;
    }
}
