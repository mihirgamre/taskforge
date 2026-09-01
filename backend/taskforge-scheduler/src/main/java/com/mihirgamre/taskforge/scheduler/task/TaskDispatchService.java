package com.mihirgamre.taskforge.scheduler.task;

import com.mihirgamre.taskforge.scheduler.automation.ScheduleDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskDispatchService {
    private final ScheduleDispatchService scheduleDispatchService;
    private final TaskClaimService claimService;

    public TaskDispatchService(ScheduleDispatchService scheduleDispatchService, TaskClaimService claimService) {
        this.scheduleDispatchService = scheduleDispatchService;
        this.claimService = claimService;
    }

    @Scheduled(fixedDelayString = "${taskforge.scheduler.poll-delay-ms:1000}")
    public void dispatchNextPendingTask() {
        scheduleDispatchService.dispatchDueSchedules();
        claimService.recoverExpiredLeases();
        claimService.claimNextPendingTask();
    }
}
