package com.mihirgamre.taskforge.domain.task;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, UUID> {
    Optional<TaskExecution> findByIdAndTenantId(UUID id, String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TaskExecution> findFirstByStatusOrderByCreatedAtAsc(TaskStatus status);
}
