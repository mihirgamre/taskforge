package com.mihirgamre.taskforge.controlplane.task;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskCommandServiceTest {

    private final TaskExecutionRepository repository = mock(TaskExecutionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC);
    private final TaskCommandService service = new TaskCommandService(repository, clock);
    private final UUID organizationId = UUID.randomUUID();

    @Test
    void createsPendingNoOpTaskForOrganization() {
        when(repository.save(any(TaskExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskExecution execution = service.createNoOp(organizationId, "phase one");

        assertThat(execution.organizationId()).isEqualTo(organizationId);
        assertThat(execution.tenantId()).isEqualTo(organizationId.toString());
        assertThat(execution.description()).isEqualTo("phase one");
        assertThat(execution.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(execution.createdAt()).isEqualTo(Instant.parse("2026-08-14T10:00:00Z"));
    }

    @Test
    void doesNotReturnTaskAcrossOrganizations() {
        UUID taskId = UUID.randomUUID();
        when(repository.findByIdAndOrganizationId(taskId, organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForOrganization(taskId, organizationId))
                .hasMessageContaining("Task not found");
    }
}
