package com.mihirgamre.taskforge.controlplane.approval;

import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTaskRepository;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.task.TaskType;
import com.mihirgamre.taskforge.domain.workflow.WorkflowProgressionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceTest {
    private final TaskExecutionRepository taskRepository = mock(TaskExecutionRepository.class);
    private final DeadLetterTaskRepository deadLetterRepository = mock(DeadLetterTaskRepository.class);
    private final WorkflowProgressionService progressionService = mock(WorkflowProgressionService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
    private final ApprovalService service = new ApprovalService(taskRepository, deadLetterRepository, progressionService, clock);

    @Test
    void approveCompletesWaitingApprovalTaskAndAdvancesWorkflow() {
        UUID organizationId = UUID.randomUUID();
        TaskExecution task = waitingApprovalTask(organizationId);
        when(taskRepository.findByIdForUpdate(task.id())).thenReturn(Optional.of(task));

        ApprovalTaskResponse response = service.approve(organizationId, task.id());

        assertThat(response.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(task.status()).isEqualTo(TaskStatus.SUCCEEDED);
        verify(progressionService).afterTerminalTask(task, Instant.parse("2026-09-01T10:00:00Z"), null);
    }

    @Test
    void rejectFailsWaitingApprovalTaskAndRecordsDeadLetter() {
        UUID organizationId = UUID.randomUUID();
        TaskExecution task = waitingApprovalTask(organizationId);
        when(taskRepository.findByIdForUpdate(task.id())).thenReturn(Optional.of(task));
        when(deadLetterRepository.existsByTaskId(task.id())).thenReturn(false);

        ApprovalTaskResponse response = service.reject(organizationId, task.id(), "not approved");

        assertThat(response.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.failureMessage()).isEqualTo("not approved");
        verify(deadLetterRepository).save(any());
        verify(progressionService).afterTerminalTask(task, Instant.parse("2026-09-01T10:00:00Z"), "not approved");
    }

    @Test
    void rejectsTaskFromDifferentOrganization() {
        TaskExecution task = waitingApprovalTask(UUID.randomUUID());
        when(taskRepository.findByIdForUpdate(task.id())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.approve(UUID.randomUUID(), task.id()))
                .isInstanceOf(ResponseStatusException.class);
    }

    private TaskExecution waitingApprovalTask(UUID organizationId) {
        TaskExecution task = TaskExecution.createWorkflowTask(
                organizationId,
                UUID.randomUUID(),
                "approval",
                "Approval",
                TaskType.APPROVAL,
                "{\"prompt\":\"Approve?\"}",
                TaskStatus.PENDING,
                Instant.parse("2026-09-01T09:00:00Z")
        );
        task.markDispatched(Instant.parse("2026-09-01T09:01:00Z"));
        task.markWaitingForApproval("{\"prompt\":\"Approve?\"}", Instant.parse("2026-09-01T09:02:00Z"));
        return task;
    }
}
