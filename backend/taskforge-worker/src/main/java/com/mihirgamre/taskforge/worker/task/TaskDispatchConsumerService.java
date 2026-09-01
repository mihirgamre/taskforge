package com.mihirgamre.taskforge.worker.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.domain.inbox.InboxEvent;
import com.mihirgamre.taskforge.domain.inbox.InboxEventRepository;
import com.mihirgamre.taskforge.domain.task.TaskDispatchEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskDispatchConsumerService {
    static final String CONSUMER_NAME = "taskforge-worker";

    private final InboxEventRepository inboxRepository;
    private final AutomationTaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String workerId;

    public TaskDispatchConsumerService(
            InboxEventRepository inboxRepository,
            AutomationTaskExecutor taskExecutor,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.inboxRepository = inboxRepository;
        this.taskExecutor = taskExecutor;
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
        return taskExecutor.execute(event.taskId(), workerId);
    }

    private TaskDispatchEvent parse(String payload) {
        try {
            return objectMapper.readValue(payload, TaskDispatchEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Malformed task dispatch event", exception);
        }
    }
}
