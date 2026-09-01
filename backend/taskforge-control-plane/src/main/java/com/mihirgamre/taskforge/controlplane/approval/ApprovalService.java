package com.mihirgamre.taskforge.controlplane.approval;

import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTask;
import com.mihirgamre.taskforge.domain.deadletter.DeadLetterTaskRepository;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.workflow.WorkflowProgressionService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApprovalService {
    private final TaskExecutionRepository taskRepository;
    private final DeadLetterTaskRepository deadLetterRepository;
    private final WorkflowProgressionService progressionService;
    private final Clock clock;

    public ApprovalService(
            TaskExecutionRepository taskRepository,
            DeadLetterTaskRepository deadLetterRepository,
            WorkflowProgressionService progressionService,
            Clock clock
    ) {
        this.taskRepository = taskRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.progressionService = progressionService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ApprovalTaskResponse> listWaiting(UUID organizationId) {
        return taskRepository.findByOrganizationIdAndStatusOrderByCreatedAtAsc(organizationId, TaskStatus.WAITING_APPROVAL)
                .stream()
                .map(ApprovalTaskResponse::from)
                .toList();
    }

    @Transactional
    public ApprovalTaskResponse approve(UUID organizationId, UUID taskId) {
        TaskExecution task = findWaitingApproval(organizationId, taskId);
        Instant now = Instant.now(clock);
        task.markApproved("{\"approved\":true}", now);
        progressionService.afterTerminalTask(task, now, null);
        return ApprovalTaskResponse.from(task);
    }

    @Transactional
    public ApprovalTaskResponse reject(UUID organizationId, UUID taskId, String reason) {
        TaskExecution task = findWaitingApproval(organizationId, taskId);
        Instant now = Instant.now(clock);
        String failure = reason == null || reason.isBlank() ? "Approval rejected" : reason;
        task.markFailedFromWaitingApproval(failure, now);
        if (!deadLetterRepository.existsByTaskId(task.id())) {
            deadLetterRepository.save(new DeadLetterTask(task.id(), failure, now));
        }
        progressionService.afterTerminalTask(task, now, failure);
        return ApprovalTaskResponse.from(task);
    }

    private TaskExecution findWaitingApproval(UUID organizationId, UUID taskId) {
        TaskExecution task = taskRepository.findByIdForUpdate(taskId)
                .filter(candidate -> organizationId.equals(candidate.organizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval task not found"));
        if (task.status() != TaskStatus.WAITING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task is not waiting for approval");
        }
        return task;
    }
}
