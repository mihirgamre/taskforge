package com.mihirgamre.taskforge.controlplane.automation;

import com.mihirgamre.taskforge.domain.automation.SimpleCron;
import com.mihirgamre.taskforge.domain.automation.WorkflowSchedule;
import com.mihirgamre.taskforge.domain.automation.WorkflowScheduleRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScheduleService {
    private final WorkflowScheduleRepository scheduleRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository versionRepository;
    private final Clock clock;

    public ScheduleService(
            WorkflowScheduleRepository scheduleRepository,
            WorkflowRepository workflowRepository,
            WorkflowVersionRepository versionRepository,
            Clock clock
    ) {
        this.scheduleRepository = scheduleRepository;
        this.workflowRepository = workflowRepository;
        this.versionRepository = versionRepository;
        this.clock = clock;
    }

    @Transactional
    public ScheduleResponse create(UUID organizationId, CreateScheduleRequest request) {
        workflowRepository.findByIdAndOrganizationId(request.workflowId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
        versionRepository.findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(request.workflowId(), WorkflowVersionStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow has no published version"));
        Instant now = Instant.now(clock);
        Instant next = nextRun(request.cronExpression(), request.timeZone(), now);
        WorkflowSchedule schedule = scheduleRepository.save(new WorkflowSchedule(
                organizationId,
                request.workflowId(),
                request.name(),
                request.cronExpression(),
                request.timeZone(),
                next,
                now
        ));
        return ScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(UUID organizationId) {
        return scheduleRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional
    public void disable(UUID organizationId, UUID scheduleId) {
        WorkflowSchedule schedule = scheduleRepository.findByIdAndOrganizationId(scheduleId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        schedule.disable(Instant.now(clock));
    }

    private Instant nextRun(String cronExpression, String timeZone, Instant now) {
        try {
            return SimpleCron.nextRun(cronExpression, timeZone, now);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
