package com.mihirgamre.taskforge.controlplane.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.task.TaskType;
import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String tenantId,
        TaskType taskType,
        TaskStatus status,
        String description,
        int attemptCount,
        Instant createdAt,
        Instant updatedAt,
        Instant dispatchedAt,
        Instant completedAt
) {
    static TaskResponse from(TaskExecution execution) {
        return new TaskResponse(
                execution.id(),
                execution.tenantId(),
                execution.taskType(),
                execution.status(),
                execution.description(),
                execution.attemptCount(),
                execution.createdAt(),
                execution.updatedAt(),
                execution.dispatchedAt(),
                execution.completedAt()
        );
    }
}
