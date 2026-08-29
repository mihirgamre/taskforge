package com.mihirgamre.taskforge.worker.task;

import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTaskRepository;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdgeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class TaskCompletionServiceTest {

    private final TaskExecutionRepository repository = mock(TaskExecutionRepository.class);
    private final DeadLetterTaskRepository deadLetterRepository = mock(DeadLetterTaskRepository.class);
    private final WorkflowEdgeRepository edgeRepository = mock(WorkflowEdgeRepository.class);
    private final WorkflowRunRepository runRepository = mock(WorkflowRunRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-14T10:02:00Z"), ZoneOffset.UTC);
    private final TaskCompletionService completionService = new TaskCompletionService(
            repository,
            deadLetterRepository,
            edgeRepository,
            runRepository,
            clock
    );

    @Test
    void completesDispatchedNoOpTask() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        assertThat(completionService.complete(execution.id())).isTrue();
        assertThat(execution.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(execution.completedAt()).isEqualTo(Instant.parse("2026-08-14T10:02:00Z"));
    }

    @Test
    void ignoresAlreadyCompletedTask() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        execution.markSucceeded(Instant.parse("2026-08-14T10:02:00Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        assertThat(completionService.complete(execution.id())).isFalse();
        assertThat(execution.status()).isEqualTo(TaskStatus.SUCCEEDED);
    }

    @Test
    void ignoresMissingTask() {
        java.util.UUID taskId = java.util.UUID.randomUUID();
        when(repository.findByIdForUpdate(taskId)).thenReturn(Optional.empty());

        assertThat(completionService.complete(taskId)).isFalse();
    }

    @Test
    void ignoresPendingTask() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        assertThat(completionService.complete(execution.id())).isFalse();
        assertThat(execution.status()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void acquiresLeaseForDispatchedTask() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        Optional<UUID> token = completionService.acquireLease(execution.id(), "worker-a", Duration.ofSeconds(30));

        assertThat(token).isPresent();
        assertThat(execution.leaseOwner()).isEqualTo("worker-a");
        assertThat(execution.leaseExpiresAt()).isEqualTo(Instant.parse("2026-08-14T10:02:30Z"));
    }

    @Test
    void completesOnlyWithMatchingLeaseToken() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        UUID token = UUID.randomUUID();
        execution.acquireLease("worker-a", token, Instant.parse("2026-08-14T10:01:30Z"), Instant.parse("2026-08-14T10:02:30Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        assertThat(completionService.complete(execution.id(), UUID.randomUUID())).isFalse();
        assertThat(execution.status()).isEqualTo(TaskStatus.DISPATCHED);

        assertThat(completionService.complete(execution.id(), token)).isTrue();
        assertThat(execution.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(execution.leaseToken()).isNull();
    }

    @Test
    void heartbeatExtendsMatchingLease() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        UUID token = UUID.randomUUID();
        execution.acquireLease("worker-a", token, Instant.parse("2026-08-14T10:01:30Z"), Instant.parse("2026-08-14T10:01:45Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        assertThat(completionService.heartbeat(execution.id(), token, Duration.ofSeconds(30))).isTrue();

        assertThat(execution.leaseHeartbeatAt()).isEqualTo(Instant.parse("2026-08-14T10:02:00Z"));
        assertThat(execution.leaseExpiresAt()).isEqualTo(Instant.parse("2026-08-14T10:02:30Z"));
    }

    @Test
    void heartbeatRejectsMismatchedLeaseToken() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        UUID token = UUID.randomUUID();
        execution.acquireLease("worker-a", token, Instant.parse("2026-08-14T10:01:30Z"), Instant.parse("2026-08-14T10:01:45Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        assertThat(completionService.heartbeat(execution.id(), UUID.randomUUID(), Duration.ofSeconds(30))).isFalse();

        assertThat(execution.leaseHeartbeatAt()).isEqualTo(Instant.parse("2026-08-14T10:01:30Z"));
        assertThat(execution.leaseExpiresAt()).isEqualTo(Instant.parse("2026-08-14T10:01:45Z"));
    }

    @Test
    void retryableFailureReturnsTaskToPendingWithBackoff() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        UUID token = UUID.randomUUID();
        execution.acquireLease("worker-a", token, Instant.parse("2026-08-14T10:01:30Z"), Instant.parse("2026-08-14T10:02:30Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));

        assertThat(completionService.failWithRetry(execution.id(), token, "boom")).isTrue();

        assertThat(execution.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(execution.failureMessage()).isEqualTo("boom");
        assertThat(execution.nextAttemptAt()).isEqualTo(Instant.parse("2026-08-14T10:02:01Z"));
        assertThat(execution.leaseToken()).isNull();
    }

    @Test
    void finalFailureDeadLettersTask() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        execution.markRetryableFailure("first", Instant.parse("2026-08-14T10:02:00Z"), Instant.parse("2026-08-14T10:02:01Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:03:00Z"));
        execution.markRetryableFailure("second", Instant.parse("2026-08-14T10:04:00Z"), Instant.parse("2026-08-14T10:04:02Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:05:00Z"));
        UUID token = UUID.randomUUID();
        execution.acquireLease("worker-a", token, Instant.parse("2026-08-14T10:01:30Z"), Instant.parse("2026-08-14T10:02:30Z"));
        when(repository.findByIdForUpdate(execution.id())).thenReturn(Optional.of(execution));
        when(deadLetterRepository.existsByTaskId(execution.id())).thenReturn(false);

        assertThat(completionService.failWithRetry(execution.id(), token, "final")).isTrue();

        assertThat(execution.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(execution.failureMessage()).isEqualTo("final");
        verify(deadLetterRepository).save(org.mockito.ArgumentMatchers.any());
    }
}
