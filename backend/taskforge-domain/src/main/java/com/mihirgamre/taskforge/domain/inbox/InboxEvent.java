package com.mihirgamre.taskforge.domain.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_event")
@IdClass(InboxEventId.class)
public class InboxEvent {
    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Id
    @Column(name = "consumer_name", nullable = false)
    private String consumerName;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected InboxEvent() {
    }

    public InboxEvent(UUID id, String eventType, String consumerName, Instant processedAt) {
        this.id = id;
        this.eventType = eventType;
        this.consumerName = consumerName;
        this.processedAt = processedAt;
    }

    public UUID id() {
        return id;
    }
}
