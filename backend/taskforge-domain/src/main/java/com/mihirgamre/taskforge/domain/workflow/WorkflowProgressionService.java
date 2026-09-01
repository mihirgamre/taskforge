package com.mihirgamre.taskforge.domain.workflow;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkflowProgressionService {
    private final TaskExecutionRepository taskRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowRunRepository runRepository;

    public WorkflowProgressionService(
            TaskExecutionRepository taskRepository,
            WorkflowEdgeRepository edgeRepository,
            WorkflowRunRepository runRepository
    ) {
        this.taskRepository = taskRepository;
        this.edgeRepository = edgeRepository;
        this.runRepository = runRepository;
    }

    public void afterTerminalTask(TaskExecution task, Instant now, String failureMessage) {
        if (task.workflowRunId() == null || task.workflowNodeKey() == null) {
            return;
        }
        WorkflowRun run = runRepository.findByIdForUpdate(task.workflowRunId()).orElse(null);
        if (run == null || run.status() != WorkflowRunStatus.RUNNING) {
            return;
        }
        if (failureMessage != null) {
            run.markFailed(failureMessage, now);
            return;
        }
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowVersionId(run.workflowVersionId());
        edges.stream()
                .filter(edge -> edge.sourceNodeKey().equals(task.workflowNodeKey()))
                .forEach(edge -> markChildReadyIfDependenciesSucceeded(run, edge, edges, now));

        List<TaskExecution> runTasks = taskRepository.findByWorkflowRunId(run.id());
        if (runTasks.stream().allMatch(candidate -> candidate.status() == TaskStatus.SUCCEEDED)) {
            run.markSucceeded(now);
        }
    }

    private void markChildReadyIfDependenciesSucceeded(
            WorkflowRun run,
            WorkflowEdge childEdge,
            List<WorkflowEdge> allEdges,
            Instant now
    ) {
        TaskExecution child = taskRepository.findByWorkflowRunIdAndWorkflowNodeKey(run.id(), childEdge.targetNodeKey())
                .orElse(null);
        if (child == null || child.status() != TaskStatus.BLOCKED) {
            return;
        }
        boolean dependenciesSucceeded = allEdges.stream()
                .filter(edge -> edge.targetNodeKey().equals(child.workflowNodeKey()))
                .allMatch(edge -> taskRepository.findByWorkflowRunIdAndWorkflowNodeKey(run.id(), edge.sourceNodeKey())
                        .filter(parent -> parent.status() == TaskStatus.SUCCEEDED)
                        .isPresent());
        if (dependenciesSucceeded) {
            child.markReady(now);
        }
    }
}
