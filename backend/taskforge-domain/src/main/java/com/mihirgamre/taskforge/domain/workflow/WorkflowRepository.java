package com.mihirgamre.taskforge.domain.workflow;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    Optional<Workflow> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
