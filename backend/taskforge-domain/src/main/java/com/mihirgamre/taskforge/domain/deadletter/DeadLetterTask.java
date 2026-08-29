package com.mihirgamre.taskforge.domain.deadletter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_task")
public class DeadLetterTask {
    @Id
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeadLetterTask() {
    }

    public DeadLetterTask(UUID taskId, String reason, Instant now) {
        this.id = UUID.randomUUID();
        this.taskId = taskId;
        this.reason = reason;
        this.createdAt = now;
    }

    public UUID taskId() {
        return taskId;
    }
}
