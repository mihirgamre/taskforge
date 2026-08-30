package com.mihirgamre.taskforge.controlplane.workflow;

import com.mihirgamre.taskforge.controlplane.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class WorkflowController {
    private final WorkflowCommandService service;

    WorkflowController(WorkflowCommandService service) {
        this.service = service;
    }

    @PostMapping("/workflows")
    ResponseEntity<WorkflowResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateWorkflowRequest request
    ) {
        user.requireWriteAccess();
        WorkflowResponse response = service.create(user.organizationId(), request);
        return ResponseEntity.created(URI.create("/api/workflows/" + response.id())).body(response);
    }

    @GetMapping("/workflows/{workflowId}")
    WorkflowResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workflowId) {
        return service.get(user.organizationId(), workflowId);
    }

    @PatchMapping("/workflows/{workflowId}/draft")
    WorkflowDraftResponse replaceDraft(
            @PathVariable UUID workflowId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateDraftWorkflowRequest request
    ) {
        user.requireWriteAccess();
        return service.replaceDraft(user.organizationId(), workflowId, request);
    }

    @PostMapping("/workflows/{workflowId}/validate")
    WorkflowValidationResponse validate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workflowId) {
        return service.validate(user.organizationId(), workflowId);
    }

    @PostMapping("/workflows/{workflowId}/publish")
    WorkflowDraftResponse publish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workflowId) {
        user.requireWriteAccess();
        return service.publish(user.organizationId(), workflowId);
    }

    @PostMapping("/workflows/{workflowId}/runs")
    ResponseEntity<WorkflowRunResponse> startRun(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID workflowId
    ) {
        user.requireWriteAccess();
        WorkflowRunResponse response = service.startRun(user.organizationId(), workflowId);
        return ResponseEntity.created(URI.create("/api/workflow-runs/" + response.id())).body(response);
    }

    @GetMapping("/workflow-runs/{runId}")
    WorkflowRunResponse getRun(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID runId) {
        return service.getRun(user.organizationId(), runId);
    }

    @GetMapping("/workflow-runs/{runId}/tasks")
    List<WorkflowTaskResponse> getRunTasks(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID runId) {
        return service.getRunTasks(user.organizationId(), runId);
    }
}
