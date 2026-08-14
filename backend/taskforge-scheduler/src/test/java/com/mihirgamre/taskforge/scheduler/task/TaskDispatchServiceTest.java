package com.mihirgamre.taskforge.scheduler.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskDispatchServiceTest {

    private final TaskExecutionRepository repository = mock(TaskExecutionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-14T10:01:00Z"), ZoneOffset.UTC);
    private final TaskClaimService service = new TaskClaimService(repository, clock);

    @Test
    void claimsOldestPendingTaskForDispatch() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        when(repository.findFirstByStatusOrderByCreatedAtAsc(TaskStatus.PENDING)).thenReturn(Optional.of(execution));

        assertThat(service.claimNextPendingTask()).contains(execution.id());
        assertThat(execution.status()).isEqualTo(TaskStatus.DISPATCHED);
        assertThat(execution.dispatchedAt()).isEqualTo(Instant.parse("2026-08-14T10:01:00Z"));
        assertThat(execution.attemptCount()).isEqualTo(1);
    }

    @Test
    void findsNoWorkWhenNoTaskIsPending() {
        when(repository.findFirstByStatusOrderByCreatedAtAsc(TaskStatus.PENDING)).thenReturn(Optional.empty());

        assertThat(service.claimNextPendingTask()).isEmpty();
    }

    @Test
    void returnsDispatchedTaskToPendingAfterPublishFailure() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        when(repository.findById(execution.id())).thenReturn(Optional.of(execution));

        service.markPending(execution.id());

        assertThat(execution.status()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void doesNotReturnCompletedTaskToPendingAfterAmbiguousPublishFailure() {
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", Instant.parse("2026-08-14T10:00:00Z"));
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        execution.markSucceeded(Instant.parse("2026-08-14T10:02:00Z"));
        when(repository.findById(execution.id())).thenReturn(Optional.of(execution));

        service.markPending(execution.id());

        assertThat(execution.status()).isEqualTo(TaskStatus.SUCCEEDED);
    }

    @Test
    void ignoresMissingTaskWhenReturningToPending() {
        UUID taskId = UUID.randomUUID();
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        service.markPending(taskId);

        assertThat(repository.findById(taskId)).isEmpty();
    }
}
