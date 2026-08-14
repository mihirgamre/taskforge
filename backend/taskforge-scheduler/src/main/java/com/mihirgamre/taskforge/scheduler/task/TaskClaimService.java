package com.mihirgamre.taskforge.scheduler.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskClaimService {
    private final TaskExecutionRepository repository;
    private final Clock clock;

    public TaskClaimService(TaskExecutionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Optional<UUID> claimNextPendingTask() {
        Optional<TaskExecution> pending = repository.findFirstByStatusOrderByCreatedAtAsc(TaskStatus.PENDING);
        pending.ifPresent(task -> task.markDispatched(Instant.now(clock)));
        return pending.map(TaskExecution::id);
    }

    @Transactional
    public void markPending(UUID taskId) {
        repository.findById(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .ifPresent(task -> task.markPending(Instant.now(clock)));
    }
}
