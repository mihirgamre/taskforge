package com.mihirgamre.taskforge.controlplane.workflow;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.task.TaskType;
import com.mihirgamre.taskforge.domain.workflow.Workflow;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdge;
import com.mihirgamre.taskforge.domain.workflow.WorkflowNode;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRun;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersion;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CreateWorkflowRequest(@NotBlank String name, String description) {
}

record UpdateDraftWorkflowRequest(
        @NotEmpty List<@Valid WorkflowNodeRequest> nodes,
        List<@Valid WorkflowEdgeRequest> edges
) {
}

record WorkflowNodeRequest(
        @NotBlank String nodeKey,
        @NotNull TaskType type,
        @NotBlank String name,
        String configuration
) {
}

record WorkflowEdgeRequest(@NotBlank String sourceNodeKey, @NotBlank String targetNodeKey) {
}

record WorkflowResponse(
        UUID id,
        String name,
        String description,
        String status,
        UUID draftVersionId,
        Integer draftVersionNumber,
        Instant createdAt,
        Instant updatedAt
) {
    static WorkflowResponse from(Workflow workflow, WorkflowVersion draft) {
        return new WorkflowResponse(
                workflow.id(),
                workflow.name(),
                workflow.description(),
                workflow.status().name(),
                draft == null ? null : draft.id(),
                draft == null ? null : draft.versionNumber(),
                workflow.createdAt(),
                workflow.updatedAt()
        );
    }
}

record WorkflowDraftResponse(
        UUID workflowId,
        UUID versionId,
        int versionNumber,
        WorkflowVersionStatus status,
        List<WorkflowNodeResponse> nodes,
        List<WorkflowEdgeResponse> edges
) {
}

record WorkflowNodeResponse(String nodeKey, TaskType type, String name, String configuration) {
    static WorkflowNodeResponse from(WorkflowNode node) {
        return new WorkflowNodeResponse(node.nodeKey(), node.type(), node.name(), node.configuration());
    }
}

record WorkflowEdgeResponse(String sourceNodeKey, String targetNodeKey) {
    static WorkflowEdgeResponse from(WorkflowEdge edge) {
        return new WorkflowEdgeResponse(edge.sourceNodeKey(), edge.targetNodeKey());
    }
}

record WorkflowValidationResponse(boolean valid, List<String> errors) {
}

record WorkflowRunResponse(
        UUID id,
        UUID workflowId,
        UUID workflowVersionId,
        WorkflowRunStatus status,
        Instant startedAt,
        Instant completedAt,
        String failureMessage
) {
    static WorkflowRunResponse from(WorkflowRun run) {
        return new WorkflowRunResponse(
                run.id(),
                run.workflowId(),
                run.workflowVersionId(),
                run.status(),
                run.startedAt(),
                run.completedAt(),
                run.failureMessage()
        );
    }
}

record WorkflowTaskResponse(UUID id, String nodeKey, TaskStatus status, int attemptCount) {
    static WorkflowTaskResponse from(TaskExecution task) {
        return new WorkflowTaskResponse(task.id(), task.workflowNodeKey(), task.status(), task.attemptCount());
    }
}
