package com.mihirgamre.taskforge.domain.task;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, UUID> {
    Optional<TaskExecution> findByIdAndTenantId(UUID id, String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task from TaskExecution task
            where task.status = :status and task.nextAttemptAt <= :now
            order by task.createdAt asc
            limit 1
            """)
    Optional<TaskExecution> findFirstReadyByStatus(@Param("status") TaskStatus status, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task from TaskExecution task
            where task.status = :status
              and task.leaseToken is not null
              and task.leaseExpiresAt <= :now
            order by task.leaseExpiresAt asc
            """)
    List<TaskExecution> findExpiredLeases(
            @Param("status") TaskStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    List<TaskExecution> findByWorkflowRunId(UUID workflowRunId);

    Optional<TaskExecution> findByWorkflowRunIdAndWorkflowNodeKey(UUID workflowRunId, String workflowNodeKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from TaskExecution task where task.id = :id")
    Optional<TaskExecution> findByIdForUpdate(@Param("id") UUID id);
}
