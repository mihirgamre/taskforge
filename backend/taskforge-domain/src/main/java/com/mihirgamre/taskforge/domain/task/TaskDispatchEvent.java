package com.mihirgamre.taskforge.domain.task;

import java.util.UUID;

public record TaskDispatchEvent(
        UUID eventId,
        int schemaVersion,
        UUID correlationId,
        UUID causationId,
        UUID taskId
) {
    public static final String EVENT_TYPE = "task.dispatch.requested";
}
