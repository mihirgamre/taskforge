package com.mihirgamre.taskforge.controlplane.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskCommandService {
    private final TaskExecutionRepository repository;
    private final Clock clock;

    public TaskCommandService(TaskExecutionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public TaskExecution createNoOp(String tenantId, String description) {
        return repository.save(TaskExecution.createNoOp(tenantId, description, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public TaskExecution getForTenant(UUID id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}
