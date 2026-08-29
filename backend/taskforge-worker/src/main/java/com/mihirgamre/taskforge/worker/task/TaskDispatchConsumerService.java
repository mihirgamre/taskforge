package com.mihirgamre.taskforge.worker.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.domain.inbox.InboxEvent;
import com.mihirgamre.taskforge.domain.inbox.InboxEventRepository;
import com.mihirgamre.taskforge.domain.task.TaskDispatchEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskDispatchConsumerService {
    static final String CONSUMER_NAME = "taskforge-worker";

    private final InboxEventRepository inboxRepository;
    private final TaskCompletionService completionService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String workerId;

    public TaskDispatchConsumerService(
            InboxEventRepository inboxRepository,
            TaskCompletionService completionService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.inboxRepository = inboxRepository;
        this.completionService = completionService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.workerId = "worker-" + UUID.randomUUID();
    }

    @Transactional
    public boolean consume(String payload) {
        TaskDispatchEvent event = parse(payload);
        if (inboxRepository.existsByIdAndConsumerName(event.eventId(), CONSUMER_NAME)) {
            return false;
        }
        inboxRepository.save(new InboxEvent(event.eventId(), TaskDispatchEvent.EVENT_TYPE, CONSUMER_NAME, Instant.now(clock)));
        return completionService.acquireLease(event.taskId(), workerId, Duration.ofSeconds(30))
                .map(token -> completionService.complete(event.taskId(), token))
                .orElse(false);
    }

    private TaskDispatchEvent parse(String payload) {
        try {
            return objectMapper.readValue(payload, TaskDispatchEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Malformed task dispatch event", exception);
        }
    }
}
