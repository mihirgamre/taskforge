package com.mihirgamre.taskforge.worker.task;

import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NoOpTaskWorker {
    private final TaskCompletionService completionService;

    public NoOpTaskWorker(TaskCompletionService completionService) {
        this.completionService = completionService;
    }

    @KafkaListener(
            topics = "${taskforge.worker.dispatch-topic:taskforge.task-dispatch.v1}",
            groupId = "${spring.kafka.consumer.group-id:taskforge-worker}"
    )
    public void executeNoOp(String taskId) {
        completionService.complete(UUID.fromString(taskId));
    }
}
