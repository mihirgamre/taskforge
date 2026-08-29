package com.mihirgamre.taskforge.worker.task;

import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTask;
import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTaskRepository;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdge;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdgeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRun;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCompletionService {
    private final TaskExecutionRepository repository;
    private final DeadLetterTaskRepository deadLetterRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowRunRepository runRepository;
    private final Clock clock;

    public TaskCompletionService(
            TaskExecutionRepository repository,
            DeadLetterTaskRepository deadLetterRepository,
            WorkflowEdgeRepository edgeRepository,
            WorkflowRunRepository runRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.deadLetterRepository = deadLetterRepository;
        this.edgeRepository = edgeRepository;
        this.runRepository = runRepository;
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
        updateWorkflowAfterTerminalTask(task, now, null);
        return true;
    }

    private boolean markFailed(TaskExecution task, String message) {
        Instant now = Instant.now(clock);
        String failure = message == null ? "Task failed" : message;
        task.markFailed(failure, now);
        recordDeadLetter(task, failure, now);
        updateWorkflowAfterTerminalTask(task, now, failure);
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
            updateWorkflowAfterTerminalTask(task, now, failure);
        }
        return true;
    }

    private void recordDeadLetter(TaskExecution task, String reason, Instant now) {
        if (!deadLetterRepository.existsByTaskId(task.id())) {
            deadLetterRepository.save(new DeadLetterTask(task.id(), reason, now));
        }
    }

    private void updateWorkflowAfterTerminalTask(TaskExecution task, Instant now, String failureMessage) {
        if (task.workflowRunId() == null || task.workflowNodeKey() == null) {
            return;
        }
        WorkflowRun run = runRepository.findByIdForUpdate(task.workflowRunId()).orElse(null);
        if (run == null || run.status() != WorkflowRunStatus.RUNNING) {
            return;
        }
        if (failureMessage != null) {
            run.markFailed(failureMessage, now);
            return;
        }
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowVersionId(run.workflowVersionId());
        edges.stream()
                .filter(edge -> edge.sourceNodeKey().equals(task.workflowNodeKey()))
                .forEach(edge -> markChildReadyIfDependenciesSucceeded(run, edge, edges, now));

        List<TaskExecution> runTasks = repository.findByWorkflowRunId(run.id());
        if (runTasks.stream().allMatch(candidate -> candidate.status() == TaskStatus.SUCCEEDED)) {
            run.markSucceeded(now);
        }
    }

    private void markChildReadyIfDependenciesSucceeded(
            WorkflowRun run,
            WorkflowEdge childEdge,
            List<WorkflowEdge> allEdges,
            Instant now
    ) {
        TaskExecution child = repository.findByWorkflowRunIdAndWorkflowNodeKey(run.id(), childEdge.targetNodeKey())
                .orElse(null);
        if (child == null || child.status() != TaskStatus.BLOCKED) {
            return;
        }
        boolean dependenciesSucceeded = allEdges.stream()
                .filter(edge -> edge.targetNodeKey().equals(child.workflowNodeKey()))
                .allMatch(edge -> repository.findByWorkflowRunIdAndWorkflowNodeKey(run.id(), edge.sourceNodeKey())
                        .filter(parent -> parent.status() == TaskStatus.SUCCEEDED)
                        .isPresent());
        if (dependenciesSucceeded) {
            child.markReady(now);
        }
    }
}
