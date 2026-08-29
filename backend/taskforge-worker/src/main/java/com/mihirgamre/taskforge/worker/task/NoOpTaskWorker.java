package com.mihirgamre.taskforge.worker.task;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NoOpTaskWorker {
    private final TaskDispatchConsumerService consumerService;

    public NoOpTaskWorker(TaskDispatchConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics = "${taskforge.worker.dispatch-topic:taskforge.task-dispatch.v1}",
            groupId = "${spring.kafka.consumer.group-id:taskforge-worker}"
    )
    public void executeNoOp(String payload) {
        consumerService.consume(payload);
    }
}
