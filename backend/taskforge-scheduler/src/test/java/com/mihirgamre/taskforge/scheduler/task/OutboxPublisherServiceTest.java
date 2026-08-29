package com.mihirgamre.taskforge.scheduler.task;

import com.mihirgamre.taskforge.domain.outbox.OutboxEvent;
import com.mihirgamre.taskforge.domain.outbox.OutboxEventRepository;
import com.mihirgamre.taskforge.domain.outbox.OutboxEventStatus;
import com.mihirgamre.taskforge.domain.task.TaskDispatchEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherServiceTest {
    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
    private final OutboxPublisherService service = new OutboxPublisherService(repository, kafkaTemplate, clock);

    @Test
    void publishesReadyOutboxEventAndMarksItPublished() {
        OutboxEvent event = event();
        when(repository.findReadyForPublish(
                OutboxEventStatus.PENDING,
                Instant.parse("2026-08-28T10:00:00Z"),
                PageRequest.of(0, 25)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(event.topic(), event.eventKey(), event.payload()))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.publishReadyEvents();

        verify(kafkaTemplate).send(event.topic(), event.eventKey(), event.payload());
        assertThat(event.status()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.publishedAt()).isEqualTo(Instant.parse("2026-08-28T10:00:00Z"));
    }

    @Test
    void failedPublishRemainsPendingWithBackoff() {
        OutboxEvent event = event();
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("kafka down"));
        when(repository.findReadyForPublish(
                OutboxEventStatus.PENDING,
                Instant.parse("2026-08-28T10:00:00Z"),
                PageRequest.of(0, 25)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(event.topic(), event.eventKey(), event.payload())).thenReturn(failed);

        service.publishReadyEvents();

        assertThat(event.status()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.attemptCount()).isEqualTo(1);
        assertThat(event.nextAttemptAt()).isEqualTo(Instant.parse("2026-08-28T10:00:01Z"));
    }

    private OutboxEvent event() {
        UUID eventId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String payload = new TaskDispatchEvent(eventId, 1, eventId, eventId, taskId).toString();
        return new OutboxEvent(eventId, "task_execution", taskId, TaskDispatchEvent.EVENT_TYPE,
                TaskClaimService.TASK_DISPATCH_TOPIC, taskId.toString(), payload, Instant.parse("2026-08-28T09:59:00Z"));
    }
}
