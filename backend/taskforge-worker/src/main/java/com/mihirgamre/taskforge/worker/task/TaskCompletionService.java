package com.mihirgamre.taskforge.worker.task;

import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTask;
import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTaskRepository;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowProgressionService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCompletionService {
    private final TaskExecutionRepository repository;
    private final DeadLetterTaskRepository deadLetterRepository;
    private final WorkflowProgressionService progressionService;
    private final Clock clock;

    public TaskCompletionService(
            TaskExecutionRepository repository,
            DeadLetterTaskRepository deadLetterRepository,
            WorkflowProgressionService progressionService,
            Clock clock
    ) {
        this.repository = repository;
        this.deadLetterRepository = deadLetterRepository;
        this.progressionService = progressionService;
        this.clock = clock;
    }

    @Transactional
    public boolean complete(UUID taskId) {
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .map(this::markSucceeded)
                .orElse(false);
    }

    @Transactional
    public Optional<UUID> acquireLease(UUID taskId, String owner, Duration leaseDuration) {
        Instant now = Instant.now(clock);
        UUID token = UUID.randomUUID();
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .filter(task -> task.acquireLease(owner, token, now, now.plus(leaseDuration)))
                .map(task -> token);
    }

    @Transactional
    public boolean heartbeat(UUID taskId, UUID leaseToken, Duration leaseDuration) {
        Instant now = Instant.now(clock);
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.heartbeat(leaseToken, now, now.plus(leaseDuration)))
                .isPresent();
    }

    @Transactional
    public boolean complete(UUID taskId, UUID leaseToken) {
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .filter(task -> task.hasLeaseToken(leaseToken))
                .map(this::markSucceeded)
                .orElse(false);
    }

    @Transactional
    public boolean complete(UUID taskId, UUID leaseToken, String result) {
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .filter(task -> task.hasLeaseToken(leaseToken))
                .map(task -> markSucceeded(task, result))
                .orElse(false);
    }

    @Transactional
    public boolean waitForApproval(UUID taskId, UUID leaseToken, String result) {
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .filter(task -> task.hasLeaseToken(leaseToken))
                .map(task -> {
                    task.markWaitingForApproval(result, Instant.now(clock));
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean fail(UUID taskId, String message) {
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .map(task -> markFailed(task, message))
                .orElse(false);
    }

    @Transactional
    public boolean failWithRetry(UUID taskId, UUID leaseToken, String message) {
        return repository.findByIdForUpdate(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .filter(task -> task.hasLeaseToken(leaseToken))
                .map(task -> markRetryableOrDeadLettered(task, message))
                .orElse(false);
    }

    private boolean markSucceeded(TaskExecution task) {
        Instant now = Instant.now(clock);
        task.markSucceeded(now);
        progressionService.afterTerminalTask(task, now, null);
        return true;
    }

    boolean markSucceeded(TaskExecution task, String result) {
        Instant now = Instant.now(clock);
        task.markSucceeded(result, now);
        progressionService.afterTerminalTask(task, now, null);
        return true;
    }

    private boolean markFailed(TaskExecution task, String message) {
        Instant now = Instant.now(clock);
        String failure = message == null ? "Task failed" : message;
        task.markFailed(failure, now);
        recordDeadLetter(task, failure, now);
        progressionService.afterTerminalTask(task, now, failure);
        return true;
    }

    private boolean markRetryableOrDeadLettered(TaskExecution task, String message) {
        Instant now = Instant.now(clock);
        String failure = message == null ? "Task failed" : message;
        if (task.canRetry()) {
            long delaySeconds = Math.min(60, 1L << Math.max(0, task.attemptCount() - 1));
            task.markRetryableFailure(failure, now, now.plusSeconds(delaySeconds));
        } else {
            task.markDeadLettered(failure, now);
            recordDeadLetter(task, failure, now);
            progressionService.afterTerminalTask(task, now, failure);
        }
        return true;
    }

    private void recordDeadLetter(TaskExecution task, String reason, Instant now) {
        if (!deadLetterRepository.existsByTaskId(task.id())) {
            deadLetterRepository.save(new DeadLetterTask(task.id(), reason, now));
        }
    }

}
