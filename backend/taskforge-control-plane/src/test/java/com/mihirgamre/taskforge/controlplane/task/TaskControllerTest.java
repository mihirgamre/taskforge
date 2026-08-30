package com.mihirgamre.taskforge.controlplane.task;

import com.mihirgamre.taskforge.controlplane.auth.AuthenticatedUser;
import com.mihirgamre.taskforge.domain.identity.OrganizationRole;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private final TaskCommandService service = mock(TaskCommandService.class);
    private final TaskController controller = new TaskController(service);
    private final UUID organizationId = UUID.randomUUID();
    private final AuthenticatedUser user = new AuthenticatedUser(
            UUID.randomUUID(),
            "user@example.com",
            organizationId,
            OrganizationRole.MEMBER
    );

    @Test
    void createsNoOpTaskResponseWithLocation() {
        Instant now = Instant.parse("2026-08-14T10:00:00Z");
        TaskExecution execution = TaskExecution.createNoOp(organizationId, "smoke", now);
        when(service.createNoOp(organizationId, "smoke")).thenReturn(execution);

        ResponseEntity<TaskResponse> response = controller.createNoOp(user, new CreateNoOpTaskRequest("smoke"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/tasks/" + execution.id());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tenantId()).isEqualTo(organizationId.toString());
    }
}
