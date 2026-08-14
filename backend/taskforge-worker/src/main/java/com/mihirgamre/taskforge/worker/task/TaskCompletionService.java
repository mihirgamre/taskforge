package com.mihirgamre.taskforge.worker.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCompletionService {
    private final TaskExecutionRepository repository;
    private final Clock clock;

    public TaskCompletionService(TaskExecutionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public boolean complete(UUID taskId) {
        return repository.findById(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .map(this::markSucceeded)
                .orElse(false);
    }

    private boolean markSucceeded(TaskExecution task) {
        task.markSucceeded(Instant.now(clock));
        return true;
    }
}
