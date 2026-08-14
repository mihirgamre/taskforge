package com.mihirgamre.taskforge.worker.task;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class NoOpTaskWorkerTest {

    private final TaskCompletionService completionService = mock(TaskCompletionService.class);
    private final NoOpTaskWorker worker = new NoOpTaskWorker(completionService);

    @Test
    void delegatesNoOpExecutionToCompletionService() {
        UUID taskId = UUID.randomUUID();
        when(completionService.complete(taskId)).thenReturn(true);

        worker.executeNoOp(taskId.toString());

        verify(completionService).complete(taskId);
        verifyNoMoreInteractions(completionService);
    }

    @Test
    void rejectsMalformedTaskId() {
        assertThatThrownBy(() -> worker.executeNoOp("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoMoreInteractions(completionService);
    }
}
