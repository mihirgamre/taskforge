package com.mihirgamre.taskforge.scheduler.automation;

import com.mihirgamre.taskforge.domain.automation.SimpleCron;
import com.mihirgamre.taskforge.domain.automation.WorkflowSchedule;
import com.mihirgamre.taskforge.domain.automation.WorkflowScheduleRepository;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleDispatchService {
    private static final int SCHEDULE_BATCH_SIZE = 10;

    private final WorkflowScheduleRepository scheduleRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowRunRepository runRepository;
    private final TaskExecutionRepository taskRepository;
    private final Clock clock;

    public ScheduleDispatchService(
            WorkflowScheduleRepository scheduleRepository,
            WorkflowVersionRepository versionRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowEdgeRepository edgeRepository,
            WorkflowRunRepository runRepository,
            TaskExecutionRepository taskRepository,
            Clock clock
    ) {
        this.scheduleRepository = scheduleRepository;
        this.versionRepository = versionRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional
    public int dispatchDueSchedules() {
        Instant now = Instant.now(clock);
        List<WorkflowSchedule> dueSchedules = scheduleRepository.findDueSchedules(
                now,
                PageRequest.of(0, SCHEDULE_BATCH_SIZE)
        );
        dueSchedules.forEach(schedule -> dispatch(schedule, now));
        return dueSchedules.size();
    }

    private void dispatch(WorkflowSchedule schedule, Instant now) {
        WorkflowVersion version = versionRepository
                .findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(
                        schedule.workflowId(),
                        WorkflowVersionStatus.PUBLISHED
                )
                .orElseThrow(() -> new IllegalStateException("Scheduled workflow has no published version"));
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowVersionId(version.id());
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowVersionId(version.id());
        Set<String> nonRootNodes = new HashSet<>();
        edges.forEach(edge -> nonRootNodes.add(edge.targetNodeKey()));

        WorkflowRun run = runRepository.save(WorkflowRun.start(
                schedule.workflowId(),
                version.id(),
                schedule.organizationId(),
                now
        ));
        taskRepository.saveAll(nodes.stream()
                .map(node -> TaskExecution.createWorkflowTask(
                        schedule.organizationId(),
                        run.id(),
                        node.nodeKey(),
                        node.name(),
                        node.type(),
                        node.configuration(),
                        nonRootNodes.contains(node.nodeKey()) ? TaskStatus.BLOCKED : TaskStatus.PENDING,
                        now
                ))
                .toList());
        schedule.recordRun(now, SimpleCron.nextRun(schedule.cronExpression(), schedule.timeZone(), now));
    }
}
