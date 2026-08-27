package com.mihirgamre.taskforge.domain.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, UUID> {
    List<WorkflowNode> findByWorkflowVersionId(UUID workflowVersionId);

    Optional<WorkflowNode> findByWorkflowVersionIdAndNodeKey(UUID workflowVersionId, String nodeKey);

    void deleteByWorkflowVersionId(UUID workflowVersionId);
}
