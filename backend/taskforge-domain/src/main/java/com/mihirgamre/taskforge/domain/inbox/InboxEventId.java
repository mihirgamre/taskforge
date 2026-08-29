package com.mihirgamre.taskforge.domain.inbox;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class InboxEventId implements Serializable {
    private UUID id;
    private String consumerName;

    protected InboxEventId() {
    }

    public InboxEventId(UUID id, String consumerName) {
        this.id = id;
        this.consumerName = consumerName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InboxEventId that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(consumerName, that.consumerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, consumerName);
    }
}
