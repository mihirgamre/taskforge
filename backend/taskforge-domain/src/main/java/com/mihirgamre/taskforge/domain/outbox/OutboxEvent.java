package com.mihirgamre.taskforge.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error")
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String eventKey,
            String payload,
            Instant now
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID aggregateId() {
        return aggregateId;
    }

    public String eventType() {
        return eventType;
    }

    public String topic() {
        return topic;
    }

    public String eventKey() {
        return eventKey;
    }

    public String payload() {
        return payload;
    }

    public OutboxEventStatus status() {
        return status;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public void markPublished(Instant now) {
        if (status == OutboxEventStatus.PUBLISHED) {
            return;
        }
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = now;
        this.lastError = null;
    }

    public void markPublishFailed(String error, Instant nextAttemptAt) {
        if (status == OutboxEventStatus.PUBLISHED) {
            return;
        }
        this.status = OutboxEventStatus.PENDING;
        this.attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error;
    }
}
