package com.mihirgamre.taskforge.domain.deadletter;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterTaskRepository extends JpaRepository<DeadLetterTask, UUID> {
    boolean existsByTaskId(UUID taskId);
}
