package com.mihirgamre.taskforge.worker.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.task.TaskType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AutomationTaskExecutorTest {
    private final TaskExecutionRepository repository = mock(TaskExecutionRepository.class);
    private final TaskCompletionService completionService = mock(TaskCompletionService.class);
    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final AutomationTaskExecutor executor = new AutomationTaskExecutor(
            repository,
            completionService,
            new ObjectMapper(),
            restClientBuilder
    );

    @Test
    void completesTransformTaskWithResult() {
        UUID token = UUID.randomUUID();
        TaskExecution task = workflowTask(TaskType.TRANSFORM, "{\"value\":{\"hello\":\"world\"}}");
        when(completionService.acquireLease(eq(task.id()), eq("worker-a"), any())).thenReturn(Optional.of(token));
        when(repository.findById(task.id())).thenReturn(Optional.of(task));
        when(completionService.complete(eq(task.id()), eq(token), any())).thenReturn(true);

        assertThat(executor.execute(task.id(), "worker-a")).isTrue();

        verify(completionService).complete(task.id(), token, "{\"transformed\":{\"hello\":\"world\"}}");
    }

    @Test
    void approvalTaskWaitsForManualDecision() {
        UUID token = UUID.randomUUID();
        TaskExecution task = workflowTask(TaskType.APPROVAL, "{\"prompt\":\"Ship it?\"}");
        when(completionService.acquireLease(eq(task.id()), eq("worker-a"), any())).thenReturn(Optional.of(token));
        when(repository.findById(task.id())).thenReturn(Optional.of(task));
        when(completionService.waitForApproval(eq(task.id()), eq(token), any())).thenReturn(true);

        assertThat(executor.execute(task.id(), "worker-a")).isTrue();

        verify(completionService).waitForApproval(task.id(), token, "{\"prompt\":\"Ship it?\"}");
    }

    @Test
    void blockedHttpTargetFailsThroughRetryPath() {
        UUID token = UUID.randomUUID();
        TaskExecution task = workflowTask(TaskType.HTTP, "{\"url\":\"http://10.0.0.5/private\"}");
        when(completionService.acquireLease(eq(task.id()), eq("worker-a"), any())).thenReturn(Optional.of(token));
        when(repository.findById(task.id())).thenReturn(Optional.of(task));
        when(completionService.failWithRetry(eq(task.id()), eq(token), any())).thenReturn(true);

        assertThat(executor.execute(task.id(), "worker-a")).isTrue();

        verify(completionService).failWithRetry(task.id(), token, "HTTP task URL targets a blocked host");
    }

    @Test
    void httpTaskPersistsOnlyStatusCode() {
        UUID token = UUID.randomUUID();
        TaskExecution task = workflowTask(TaskType.HTTP, "{\"url\":\"https://example.com/hook\"}");
        when(completionService.acquireLease(eq(task.id()), eq("worker-a"), any())).thenReturn(Optional.of(token));
        when(repository.findById(task.id())).thenReturn(Optional.of(task));
        when(completionService.complete(eq(task.id()), eq(token), any())).thenReturn(true);
        server.expect(requestTo("https://example.com/hook")).andRespond(withSuccess("secret-token-value", MediaType.TEXT_PLAIN));

        assertThat(executor.execute(task.id(), "worker-a")).isTrue();

        verify(completionService).complete(task.id(), token, "{\"statusCode\":200}");
        server.verify();
    }

    private TaskExecution workflowTask(TaskType type, String configuration) {
        TaskExecution task = TaskExecution.createWorkflowTask(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "A",
                "Task A",
                type,
                configuration,
                TaskStatus.PENDING,
                Instant.parse("2026-09-01T10:00:00Z")
        );
        task.markDispatched(Instant.parse("2026-09-01T10:01:00Z"));
        return task;
    }
}
