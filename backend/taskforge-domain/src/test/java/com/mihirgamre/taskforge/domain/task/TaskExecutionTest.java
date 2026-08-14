package com.mihirgamre.taskforge.domain.task;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskExecutionTest {

    @Test
    void recordsNoOpTaskLifecycleTimestamps() {
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");
        Instant dispatchedAt = Instant.parse("2026-08-14T10:01:00Z");
        Instant completedAt = Instant.parse("2026-08-14T10:02:00Z");

        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", createdAt);
        execution.markDispatched(dispatchedAt);
        execution.markSucceeded(completedAt);

        assertThat(execution.taskType()).isEqualTo(TaskType.NO_OP);
        assertThat(execution.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(execution.attemptCount()).isEqualTo(1);
        assertThat(execution.createdAt()).isEqualTo(createdAt);
        assertThat(execution.dispatchedAt()).isEqualTo(dispatchedAt);
        assertThat(execution.completedAt()).isEqualTo(completedAt);
    }

    @Test
    void rejectsDispatchFromTerminalTask() {
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", createdAt);
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        execution.markSucceeded(Instant.parse("2026-08-14T10:02:00Z"));

        assertThatThrownBy(() -> execution.markDispatched(Instant.parse("2026-08-14T10:03:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot dispatch task")
                .hasMessageContaining("SUCCEEDED");
    }

    @Test
    void rejectsReturningTerminalTaskToPending() {
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", createdAt);
        execution.markDispatched(Instant.parse("2026-08-14T10:01:00Z"));
        execution.markSucceeded(Instant.parse("2026-08-14T10:02:00Z"));

        assertThatThrownBy(() -> execution.markPending(Instant.parse("2026-08-14T10:03:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot return to pending task")
                .hasMessageContaining("SUCCEEDED");
    }

    @Test
    void rejectsCompletionBeforeDispatch() {
        TaskExecution execution = TaskExecution.createNoOp(
                "tenant-a",
                "smoke",
                Instant.parse("2026-08-14T10:00:00Z")
        );

        assertThatThrownBy(() -> execution.markSucceeded(Instant.parse("2026-08-14T10:01:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot succeed task")
                .hasMessageContaining("PENDING");
    }
}
