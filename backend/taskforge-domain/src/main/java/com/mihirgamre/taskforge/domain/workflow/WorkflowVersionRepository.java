package com.mihirgamre.taskforge.domain.workflow;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {
    Optional<WorkflowVersion> findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(
            UUID workflowId,
            WorkflowVersionStatus status
    );

    Optional<WorkflowVersion> findFirstByWorkflowIdOrderByVersionNumberDesc(UUID workflowId);
}
