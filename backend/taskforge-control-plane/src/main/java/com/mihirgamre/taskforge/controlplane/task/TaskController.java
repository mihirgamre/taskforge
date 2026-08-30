package com.mihirgamre.taskforge.controlplane.task;

import com.mihirgamre.taskforge.controlplane.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody(required = false) CreateNoOpTaskRequest request
    ) {
        user.requireWriteAccess();
        String description = request == null ? null : request.description();
        TaskResponse response = TaskResponse.from(service.createNoOp(user.organizationId(), description));
        return ResponseEntity.created(URI.create("/api/tasks/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    TaskResponse getTask(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        return TaskResponse.from(service.getForOrganization(id, user.organizationId()));
    }
}
