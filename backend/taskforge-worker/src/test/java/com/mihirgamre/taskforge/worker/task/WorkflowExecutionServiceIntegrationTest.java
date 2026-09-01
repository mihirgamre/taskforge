package com.mihirgamre.taskforge.worker.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.task.TaskType;
import com.mihirgamre.taskforge.domain.workflow.Workflow;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdge;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdgeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowNode;
import com.mihirgamre.taskforge.domain.workflow.WorkflowNodeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRun;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersion;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowProgressionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        classes = WorkflowExecutionServiceIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=true",
                "otel.sdk.disabled=true"
        }
)
class WorkflowExecutionServiceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.4");

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowVersionRepository versionRepository;

    @Autowired
    private WorkflowNodeRepository nodeRepository;

    @Autowired
    private WorkflowEdgeRepository edgeRepository;

    @Autowired
    private WorkflowRunRepository runRepository;

    @Autowired
    private TaskExecutionRepository taskRepository;

    @Autowired
    private TaskCompletionService completionService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void linearWorkflowUnlocksNodesInDependencyOrderAndCompletesRun() {
        WorkflowRun run = createRun(List.of("A", "B", "C"), List.of(edge("A", "B"), edge("B", "C")));

        dispatchAndComplete(run.id(), "A");
        assertStatuses(run.id(), Map.of("A", TaskStatus.SUCCEEDED, "B", TaskStatus.PENDING, "C", TaskStatus.BLOCKED));

        dispatchAndComplete(run.id(), "B");
        assertStatuses(run.id(), Map.of("A", TaskStatus.SUCCEEDED, "B", TaskStatus.SUCCEEDED, "C", TaskStatus.PENDING));

        dispatchAndComplete(run.id(), "C");
        assertStatuses(run.id(), Map.of("A", TaskStatus.SUCCEEDED, "B", TaskStatus.SUCCEEDED, "C", TaskStatus.SUCCEEDED));
        assertThat(runRepository.findById(run.id()).orElseThrow().status()).isEqualTo(WorkflowRunStatus.SUCCEEDED);
    }

    @Test
    void fanOutChildrenBecomeEligibleAfterParentSucceeds() {
        WorkflowRun run = createRun(List.of("A", "B", "C"), List.of(edge("A", "B"), edge("A", "C")));

        dispatchAndComplete(run.id(), "A");

        assertStatuses(run.id(), Map.of("A", TaskStatus.SUCCEEDED, "B", TaskStatus.PENDING, "C", TaskStatus.PENDING));
    }

    @Test
    void fanInChildWaitsForAllParents() {
        WorkflowRun run = createRun(List.of("A", "B", "C"), List.of(edge("A", "C"), edge("B", "C")));

        dispatchAndComplete(run.id(), "A");
        assertStatuses(run.id(), Map.of("A", TaskStatus.SUCCEEDED, "B", TaskStatus.PENDING, "C", TaskStatus.BLOCKED));

        dispatchAndComplete(run.id(), "B");
        assertStatuses(run.id(), Map.of("A", TaskStatus.SUCCEEDED, "B", TaskStatus.SUCCEEDED, "C", TaskStatus.PENDING));
    }

    @Test
    void concurrentFinalPredecessorsDoNotScheduleChildTwice() throws Exception {
        WorkflowRun run = createRun(List.of("A", "B", "C"), List.of(edge("A", "C"), edge("B", "C")));
        dispatch(run.id(), "A");
        dispatch(run.id(), "B");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> completeA = () -> {
            start.await();
            return completionService.complete(task(run.id(), "A").id());
        };
        Callable<Boolean> completeB = () -> {
            start.await();
            return completionService.complete(task(run.id(), "B").id());
        };

        try {
            var first = executor.submit(completeA);
            var second = executor.submit(completeB);
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, true);
            assertStatuses(run.id(), Map.of("A", TaskStatus.SUCCEEDED, "B", TaskStatus.SUCCEEDED, "C", TaskStatus.PENDING));
            assertThat(taskRepository.findByWorkflowRunId(run.id()).stream()
                    .filter(task -> "C".equals(task.workflowNodeKey())))
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedRequiredTaskFailsRunAndLeavesDescendantBlocked() {
        WorkflowRun run = createRun(List.of("A", "B"), List.of(edge("A", "B")));
        dispatch(run.id(), "A");

        assertThat(completionService.fail(task(run.id(), "A").id(), "boom")).isTrue();

        assertStatuses(run.id(), Map.of("A", TaskStatus.FAILED, "B", TaskStatus.BLOCKED));
        assertThat(runRepository.findById(run.id()).orElseThrow().status()).isEqualTo(WorkflowRunStatus.FAILED);
    }

    private WorkflowRun createRun(List<String> nodeKeys, List<WorkflowEdge> edgeTemplates) {
        Instant now = Instant.parse("2026-08-26T10:00:00Z");
        Workflow workflow = workflowRepository.save(Workflow.create("workflow", null, now));
        WorkflowVersion version = versionRepository.save(WorkflowVersion.draft(workflow.id(), 1, now));
        nodeRepository.saveAll(nodeKeys.stream()
                .map(nodeKey -> new WorkflowNode(version.id(), nodeKey, TaskType.NO_OP, nodeKey, "{}", now))
                .toList());
        edgeRepository.saveAll(edgeTemplates.stream()
                .map(edge -> new WorkflowEdge(version.id(), edge.sourceNodeKey(), edge.targetNodeKey()))
                .toList());
        version.publish(now);
        WorkflowRun run = runRepository.save(WorkflowRun.start(workflow.id(), version.id(), now));
        var childKeys = edgeTemplates.stream().map(WorkflowEdge::targetNodeKey).collect(Collectors.toSet());
        taskRepository.saveAll(nodeKeys.stream()
                .map(nodeKey -> TaskExecution.createWorkflowNoOp(
                        run.id(),
                        nodeKey,
                        nodeKey,
                        childKeys.contains(nodeKey) ? TaskStatus.BLOCKED : TaskStatus.PENDING,
                        now
                ))
                .toList());
        return run;
    }

    private void dispatchAndComplete(UUID runId, String nodeKey) {
        dispatch(runId, nodeKey);
        assertThat(completionService.complete(task(runId, nodeKey).id())).isTrue();
    }

    private void dispatch(UUID runId, String nodeKey) {
        TaskExecution task = task(runId, nodeKey);
        task.markDispatched(Instant.parse("2026-08-26T10:01:00Z"));
        taskRepository.save(task);
    }

    private TaskExecution task(UUID runId, String nodeKey) {
        return taskRepository.findByWorkflowRunIdAndWorkflowNodeKey(runId, nodeKey).orElseThrow();
    }

    private void assertStatuses(UUID runId, Map<String, TaskStatus> expectedStatuses) {
        Map<String, TaskStatus> actual = taskRepository.findByWorkflowRunId(runId).stream()
                .collect(Collectors.toMap(TaskExecution::workflowNodeKey, TaskExecution::status));
        assertThat(actual).containsAllEntriesOf(expectedStatuses);
    }

    private WorkflowEdge edge(String source, String target) {
        return new WorkflowEdge(UUID.randomUUID(), source, target);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("com.mihirgamre.taskforge.domain")
    @EnableJpaRepositories("com.mihirgamre.taskforge.domain")
    @Import({TaskCompletionService.class, WorkflowProgressionService.class})
    static class TestApplication {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-26T10:02:00Z"), ZoneOffset.UTC);
        }
    }
}
