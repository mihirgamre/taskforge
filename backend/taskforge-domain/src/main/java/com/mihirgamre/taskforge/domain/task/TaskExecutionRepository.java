package com.mihirgamre.taskforge.domain.task;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, UUID> {
    Optional<TaskExecution> findByIdAndTenantId(UUID id, String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TaskExecution> findFirstByStatusOrderByCreatedAtAsc(TaskStatus status);

    List<TaskExecution> findByWorkflowRunId(UUID workflowRunId);

    Optional<TaskExecution> findByWorkflowRunIdAndWorkflowNodeKey(UUID workflowRunId, String workflowNodeKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from TaskExecution task where task.id = :id")
    Optional<TaskExecution> findByIdForUpdate(@Param("id") UUID id);
}
