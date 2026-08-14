package com.mihirgamre.taskforge.controlplane.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/tasks")
class TaskController {
    private final TaskCommandService service;

    TaskController(TaskCommandService service) {
        this.service = service;
    }

    @PostMapping("/noop")
    ResponseEntity<TaskResponse> createNoOp(
            @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
            @Valid @RequestBody(required = false) CreateNoOpTaskRequest request
    ) {
        String description = request == null ? null : request.description();
        TaskResponse response = TaskResponse.from(service.createNoOp(tenantId, description));
        return ResponseEntity.created(URI.create("/api/tasks/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    TaskResponse getTask(
            @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
            @PathVariable UUID id
    ) {
        return TaskResponse.from(service.getForTenant(id, tenantId));
    }
}
