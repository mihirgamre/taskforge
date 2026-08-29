package com.mihirgamre.taskforge.scheduler.task;

import com.mihirgamre.taskforge.domain.outbox.OutboxEvent;
import com.mihirgamre.taskforge.domain.outbox.OutboxEventRepository;
import com.mihirgamre.taskforge.domain.outbox.OutboxEventStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisherService {
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    public OutboxPublisherService(
            OutboxEventRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${taskforge.scheduler.outbox-poll-delay-ms:500}")
    @Transactional
    public void publishReadyEvents() {
        Instant now = Instant.now(clock);
        outboxRepository.findReadyForPublish(OutboxEventStatus.PENDING, now, PageRequest.of(0, 25))
                .forEach(event -> publish(event, now));
    }

    void publish(OutboxEvent event, Instant now) {
        try {
            kafkaTemplate.send(event.topic(), event.eventKey(), event.payload()).get();
            event.markPublished(now);
        } catch (Exception exception) {
            Duration backoff = Duration.ofSeconds(Math.min(60, 1L << Math.min(event.attemptCount(), 5)));
            event.markPublishFailed(exception.getMessage(), now.plus(backoff));
        }
    }
}
