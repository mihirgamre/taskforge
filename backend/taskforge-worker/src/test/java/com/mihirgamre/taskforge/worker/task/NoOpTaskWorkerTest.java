package com.mihirgamre.taskforge.worker.task;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NoOpTaskWorkerTest {

    private final TaskDispatchConsumerService consumerService = mock(TaskDispatchConsumerService.class);
    private final NoOpTaskWorker worker = new NoOpTaskWorker(consumerService);

    @Test
    void delegatesNoOpExecutionToConsumerService() {
        String payload = "{\"eventId\":\"d7950553-b3a2-4d4a-a75c-0eb4cb62af02\",\"schemaVersion\":1}";

        worker.executeNoOp(payload);

        verify(consumerService).consume(payload);
    }
}
