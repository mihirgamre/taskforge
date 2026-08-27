package com.mihirgamre.taskforge.domain.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEdgeRepository extends JpaRepository<WorkflowEdge, UUID> {
    List<WorkflowEdge> findByWorkflowVersionId(UUID workflowVersionId);

    List<WorkflowEdge> findByWorkflowVersionIdAndTargetNodeKey(UUID workflowVersionId, String targetNodeKey);

    void deleteByWorkflowVersionId(UUID workflowVersionId);
}
