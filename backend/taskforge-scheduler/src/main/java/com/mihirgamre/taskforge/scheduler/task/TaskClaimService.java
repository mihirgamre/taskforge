package com.mihirgamre.taskforge.scheduler.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTask;
import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTaskRepository;
import com.mihirgamre.taskforge.domain.outbox.OutboxEvent;
import com.mihirgamre.taskforge.domain.outbox.OutboxEventRepository;
import com.mihirgamre.taskforge.domain.task.TaskDispatchEvent;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRun;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskClaimService {
    static final String TASK_DISPATCH_TOPIC = "taskforge.task-dispatch.v1";

    private final TaskExecutionRepository repository;
    private final OutboxEventRepository outboxRepository;
    private final DeadLetterTaskRepository deadLetterRepository;
    private final WorkflowRunRepository runRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TaskClaimService(
            TaskExecutionRepository repository,
            OutboxEventRepository outboxRepository,
            DeadLetterTaskRepository deadLetterRepository,
            WorkflowRunRepository runRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public Optional<UUID> claimNextPendingTask() {
        Instant now = Instant.now(clock);
        Optional<TaskExecution> pending = repository.findFirstReadyByStatus(TaskStatus.PENDING, now);
        pending.ifPresent(task -> {
            task.markDispatched(now);
            outboxRepository.save(dispatchEventFor(task, now));
        });
        return pending.map(TaskExecution::id);
    }

    @Transactional
    public int recoverExpiredLeases() {
        Instant now = Instant.now(clock);
        return repository.findExpiredLeases(TaskStatus.DISPATCHED, now, PageRequest.of(0, 25)).stream()
                .mapToInt(task -> recoverExpiredLease(task, now))
                .sum();
    }

    @Transactional
    public void markPending(UUID taskId) {
        repository.findById(taskId)
                .filter(task -> task.status() == TaskStatus.DISPATCHED)
                .ifPresent(task -> task.markPending(Instant.now(clock)));
    }

    private OutboxEvent dispatchEventFor(TaskExecution task, Instant now) {
        UUID eventId = UUID.randomUUID();
        TaskDispatchEvent event = new TaskDispatchEvent(eventId, 1, eventId, eventId, task.id());
        try {
            return new OutboxEvent(
                    eventId,
                    "task_execution",
                    task.id(),
                    TaskDispatchEvent.EVENT_TYPE,
                    TASK_DISPATCH_TOPIC,
                    task.id().toString(),
                    objectMapper.writeValueAsString(event),
                    now
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize task dispatch event for " + task.id(), exception);
        }
    }

    private int recoverExpiredLease(TaskExecution task, Instant now) {
        if (task.canRetry()) {
            task.markRetryableFailure("Worker lease expired", now, now);
        } else {
            task.markDeadLettered("Worker lease expired", now);
            recordDeadLetter(task, "Worker lease expired", now);
            if (task.workflowRunId() != null) {
                WorkflowRun run = runRepository.findByIdForUpdate(task.workflowRunId()).orElse(null);
                if (run != null && run.status() == WorkflowRunStatus.RUNNING) {
                    run.markFailed("Worker lease expired", now);
                }
            }
        }
        return 1;
    }

    private void recordDeadLetter(TaskExecution task, String reason, Instant now) {
        if (!deadLetterRepository.existsByTaskId(task.id())) {
            deadLetterRepository.save(new DeadLetterTask(task.id(), reason, now));
        }
    }
}
