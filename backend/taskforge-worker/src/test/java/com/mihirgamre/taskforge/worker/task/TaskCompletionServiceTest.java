package com.mihirgamre.taskforge.worker.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdgeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskCompletionServiceTest {

    private final TaskExecutionRepository repository = mock(TaskExecutionRepository.class);
    private final WorkflowEdgeRepository edgeRepository = mock(WorkflowEdgeRepository.class);
    private final WorkflowRunRepository runRepository = mock(WorkflowRunRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-14T10:02:00Z"), ZoneOffset.UTC);
    private final TaskCompletionService completionService = new TaskCompletionService(
            repository,
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
}
