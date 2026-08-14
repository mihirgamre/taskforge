package com.mihirgamre.taskforge.scheduler.task;

import java.util.Optional;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskDispatchService {
    public static final String TASK_DISPATCH_TOPIC = "taskforge.task-dispatch.v1";

    private final TaskClaimService claimService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TaskDispatchService(TaskClaimService claimService, KafkaTemplate<String, String> kafkaTemplate) {
        this.claimService = claimService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${taskforge.scheduler.poll-delay-ms:1000}")
    public void dispatchNextPendingTask() {
        Optional<UUID> taskId = claimService.claimNextPendingTask();
        taskId.ifPresent(this::publishDispatch);
    }

    void publishDispatch(UUID taskId) {
        try {
            kafkaTemplate.send(TASK_DISPATCH_TOPIC, taskId.toString(), taskId.toString()).get();
        } catch (Exception exception) {
            claimService.markPending(taskId);
            throw new IllegalStateException("Failed to dispatch task " + taskId, exception);
        }
    }
}
