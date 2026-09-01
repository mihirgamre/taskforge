package com.mihirgamre.taskforge.controlplane.approval;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import java.util.UUID;

record ApprovalTaskResponse(
        UUID id,
        UUID workflowRunId,
        String nodeKey,
        String description,
        TaskStatus status,
        String prompt
) {
    static ApprovalTaskResponse from(TaskExecution task) {
        return new ApprovalTaskResponse(
                task.id(),
                task.workflowRunId(),
                task.workflowNodeKey(),
                task.description(),
                task.status(),
                task.taskResult()
        );
    }
}

record RejectApprovalRequest(String reason) {
}
