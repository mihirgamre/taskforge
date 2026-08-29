package com.mihirgamre.taskforge.worker.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.domain.inbox.InboxEventRepository;
import com.mihirgamre.taskforge.domain.task.TaskDispatchEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskDispatchConsumerServiceTest {
    private final InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
    private final TaskCompletionService completionService = mock(TaskCompletionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
    private final TaskDispatchConsumerService service = new TaskDispatchConsumerService(
            inboxRepository,
            completionService,
            objectMapper,
            clock
    );

    @Test
    void consumesDispatchEventOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new TaskDispatchEvent(eventId, 1, eventId, eventId, taskId));
        when(inboxRepository.existsByIdAndConsumerName(eventId, TaskDispatchConsumerService.CONSUMER_NAME))
                .thenReturn(false);
        when(completionService.acquireLease(any(), any(), any())).thenReturn(Optional.of(token));
        when(completionService.complete(taskId, token)).thenReturn(true);

        assertThat(service.consume(payload)).isTrue();

        verify(inboxRepository).save(any());
        verify(completionService).complete(taskId, token);
    }

    @Test
    void ignoresAlreadyProcessedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new TaskDispatchEvent(eventId, 1, eventId, eventId, taskId));
        when(inboxRepository.existsByIdAndConsumerName(eventId, TaskDispatchConsumerService.CONSUMER_NAME))
                .thenReturn(true);

        assertThat(service.consume(payload)).isFalse();

        verify(completionService, never()).acquireLease(any(), any(), any());
    }

    @Test
    void rejectsMalformedEvent() {
        assertThatThrownBy(() -> service.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
