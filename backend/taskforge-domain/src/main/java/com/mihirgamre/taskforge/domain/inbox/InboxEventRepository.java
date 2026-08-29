package com.mihirgamre.taskforge.domain.inbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventRepository extends JpaRepository<InboxEvent, InboxEventId> {
    boolean existsByIdAndConsumerName(UUID id, String consumerName);
}
