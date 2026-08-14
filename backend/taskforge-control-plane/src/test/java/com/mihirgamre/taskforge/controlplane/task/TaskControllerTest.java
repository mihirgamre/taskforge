package com.mihirgamre.taskforge.controlplane.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private final TaskCommandService service = mock(TaskCommandService.class);
    private final TaskController controller = new TaskController(service);

    @Test
    void createsNoOpTaskResponseWithLocation() {
        Instant now = Instant.parse("2026-08-14T10:00:00Z");
        TaskExecution execution = TaskExecution.createNoOp("tenant-a", "smoke", now);
        when(service.createNoOp("tenant-a", "smoke")).thenReturn(execution);

        ResponseEntity<TaskResponse> response = controller.createNoOp("tenant-a", new CreateNoOpTaskRequest("smoke"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/tasks/" + execution.id());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tenantId()).isEqualTo("tenant-a");
    }
}
