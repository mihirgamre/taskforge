package com.mihirgamre.taskforge.worker.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.domain.inbox.InboxEventRepository;
import com.mihirgamre.taskforge.domain.task.TaskDispatchEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskDispatchConsumerServiceTest {
    private final InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
    private final AutomationTaskExecutor taskExecutor = mock(AutomationTaskExecutor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
    private final TaskDispatchConsumerService service = new TaskDispatchConsumerService(
            inboxRepository,
            taskExecutor,
            objectMapper,
            clock
    );

    @Test
    void consumesDispatchEventOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new TaskDispatchEvent(eventId, 1, eventId, eventId, taskId));
        when(inboxRepository.existsByIdAndConsumerName(eventId, TaskDispatchConsumerService.CONSUMER_NAME))
                .thenReturn(false);
        when(taskExecutor.execute(eq(taskId), any())).thenReturn(true);

        assertThat(service.consume(payload)).isTrue();

        verify(inboxRepository).save(any());
        verify(taskExecutor).execute(eq(taskId), any());
    }

    @Test
    void ignoresAlreadyProcessedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new TaskDispatchEvent(eventId, 1, eventId, eventId, taskId));
        when(inboxRepository.existsByIdAndConsumerName(eventId, TaskDispatchConsumerService.CONSUMER_NAME))
                .thenReturn(true);

        assertThat(service.consume(payload)).isFalse();

        verify(taskExecutor, never()).execute(any(), any());
    }

    @Test
    void rejectsMalformedEvent() {
        assertThatThrownBy(() -> service.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
