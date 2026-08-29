package com.mihirgamre.taskforge.scheduler.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskDispatchService {
    private final TaskClaimService claimService;

    public TaskDispatchService(TaskClaimService claimService) {
        this.claimService = claimService;
    }

    @Scheduled(fixedDelayString = "${taskforge.scheduler.poll-delay-ms:1000}")
    public void dispatchNextPendingTask() {
        claimService.recoverExpiredLeases();
        claimService.claimNextPendingTask();
    }
}
