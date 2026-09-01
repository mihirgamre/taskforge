package com.mihirgamre.taskforge.scheduler.automation;

import com.mihirgamre.taskforge.domain.automation.WorkflowSchedule;
import com.mihirgamre.taskforge.domain.automation.WorkflowScheduleRepository;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.task.TaskType;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdge;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdgeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowNode;
import com.mihirgamre.taskforge.domain.workflow.WorkflowNodeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRun;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersion;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleDispatchServiceTest {
    private final WorkflowScheduleRepository scheduleRepository = mock(WorkflowScheduleRepository.class);
    private final WorkflowVersionRepository versionRepository = mock(WorkflowVersionRepository.class);
    private final WorkflowNodeRepository nodeRepository = mock(WorkflowNodeRepository.class);
    private final WorkflowEdgeRepository edgeRepository = mock(WorkflowEdgeRepository.class);
    private final WorkflowRunRepository runRepository = mock(WorkflowRunRepository.class);
    private final TaskExecutionRepository taskRepository = mock(TaskExecutionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
    private final ScheduleDispatchService service = new ScheduleDispatchService(
            scheduleRepository,
            versionRepository,
            nodeRepository,
            edgeRepository,
            runRepository,
            taskRepository,
            clock
    );

    @Test
    void createsWorkflowRunAndTasksForDueSchedule() {
        UUID organizationId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        WorkflowSchedule schedule = new WorkflowSchedule(
                organizationId,
                workflowId,
                "Every minute",
                "* * * * *",
                "UTC",
                Instant.parse("2026-09-01T09:59:00Z"),
                Instant.parse("2026-09-01T09:00:00Z")
        );
        WorkflowVersion version = WorkflowVersion.draft(workflowId, 1, Instant.parse("2026-09-01T09:00:00Z"));
        version.publish(Instant.parse("2026-09-01T09:01:00Z"));
        WorkflowRun run = WorkflowRun.start(workflowId, version.id(), organizationId, Instant.parse("2026-09-01T10:00:00Z"));
        when(scheduleRepository.findDueSchedules(eq(Instant.parse("2026-09-01T10:00:00Z")), any())).thenReturn(List.of(schedule));
        when(versionRepository.findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(workflowId, WorkflowVersionStatus.PUBLISHED))
                .thenReturn(Optional.of(version));
        when(nodeRepository.findByWorkflowVersionId(version.id())).thenReturn(List.of(
                new WorkflowNode(version.id(), "A", TaskType.NO_OP, "A", "{}", Instant.parse("2026-09-01T09:00:00Z")),
                new WorkflowNode(version.id(), "B", TaskType.TRANSFORM, "B", "{\"value\":1}", Instant.parse("2026-09-01T09:00:00Z"))
        ));
        when(edgeRepository.findByWorkflowVersionId(version.id())).thenReturn(List.of(new WorkflowEdge(version.id(), "A", "B")));
        when(runRepository.save(any())).thenReturn(run);

        assertThat(service.dispatchDueSchedules()).isEqualTo(1);

        ArgumentCaptor<List<TaskExecution>> tasks = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(tasks.capture());
        assertThat(tasks.getValue()).hasSize(2);
        assertThat(tasks.getValue())
                .filteredOn(task -> task.workflowNodeKey().equals("A"))
                .singleElement()
                .satisfies(task -> assertThat(task.status()).isEqualTo(TaskStatus.PENDING));
        assertThat(tasks.getValue())
                .filteredOn(task -> task.workflowNodeKey().equals("B"))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.status()).isEqualTo(TaskStatus.BLOCKED);
                    assertThat(task.taskType()).isEqualTo(TaskType.TRANSFORM);
                    assertThat(task.taskConfiguration()).isEqualTo("{\"value\":1}");
                });
        assertThat(schedule.lastRunAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
        assertThat(schedule.nextRunAt()).isEqualTo(Instant.parse("2026-09-01T10:01:00Z"));
    }
}
