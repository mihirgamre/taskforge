package com.mihirgamre.taskforge.controlplane.workflow;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
    ResponseEntity<WorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request) {
        WorkflowResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/workflows/" + response.id())).body(response);
    }

    @GetMapping("/workflows/{workflowId}")
    WorkflowResponse get(@PathVariable UUID workflowId) {
        return service.get(workflowId);
    }

    @PatchMapping("/workflows/{workflowId}/draft")
    WorkflowDraftResponse replaceDraft(
            @PathVariable UUID workflowId,
            @Valid @RequestBody UpdateDraftWorkflowRequest request
    ) {
        return service.replaceDraft(workflowId, request);
    }

    @PostMapping("/workflows/{workflowId}/validate")
    WorkflowValidationResponse validate(@PathVariable UUID workflowId) {
        return service.validate(workflowId);
    }

    @PostMapping("/workflows/{workflowId}/publish")
    WorkflowDraftResponse publish(@PathVariable UUID workflowId) {
        return service.publish(workflowId);
    }

    @PostMapping("/workflows/{workflowId}/runs")
    ResponseEntity<WorkflowRunResponse> startRun(@PathVariable UUID workflowId) {
        WorkflowRunResponse response = service.startRun(workflowId);
        return ResponseEntity.created(URI.create("/api/workflow-runs/" + response.id())).body(response);
    }

    @GetMapping("/workflow-runs/{runId}")
    WorkflowRunResponse getRun(@PathVariable UUID runId) {
        return service.getRun(runId);
    }

    @GetMapping("/workflow-runs/{runId}/tasks")
    List<WorkflowTaskResponse> getRunTasks(@PathVariable UUID runId) {
        return service.getRunTasks(runId);
    }
}
