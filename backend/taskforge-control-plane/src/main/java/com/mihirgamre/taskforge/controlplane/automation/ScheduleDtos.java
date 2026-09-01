package com.mihirgamre.taskforge.controlplane.automation;

import com.mihirgamre.taskforge.domain.automation.WorkflowSchedule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

record CreateScheduleRequest(
        @NotNull UUID workflowId,
        @NotBlank String name,
        @NotBlank String cronExpression,
        @NotBlank String timeZone
) {
}

record ScheduleResponse(
        UUID id,
        UUID workflowId,
        String name,
        String cronExpression,
        String timeZone,
        boolean enabled,
        Instant nextRunAt,
        Instant lastRunAt
) {
    static ScheduleResponse from(WorkflowSchedule schedule) {
        return new ScheduleResponse(
                schedule.id(),
                schedule.workflowId(),
                schedule.name(),
                schedule.cronExpression(),
                schedule.timeZone(),
                schedule.enabled(),
                schedule.nextRunAt(),
                schedule.lastRunAt()
        );
    }
}
