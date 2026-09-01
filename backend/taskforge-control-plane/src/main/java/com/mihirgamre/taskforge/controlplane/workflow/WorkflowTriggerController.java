package com.mihirgamre.taskforge.controlplane.workflow;

import com.mihirgamre.taskforge.controlplane.automation.ApiKeyService;
import com.mihirgamre.taskforge.domain.automation.ApiKey;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/triggers")
class WorkflowTriggerController {
    private final ApiKeyService apiKeyService;
    private final WorkflowCommandService workflowCommandService;

    WorkflowTriggerController(ApiKeyService apiKeyService, WorkflowCommandService workflowCommandService) {
        this.apiKeyService = apiKeyService;
        this.workflowCommandService = workflowCommandService;
    }

    @PostMapping("/workflows/{workflowId}/runs")
    ResponseEntity<WorkflowRunResponse> startRun(
            @RequestHeader(name = "X-TaskForge-Api-Key", required = false) String rawKey,
            @PathVariable UUID workflowId
    ) {
        ApiKey apiKey = apiKeyService.authenticate(rawKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid API key is required"));
        WorkflowRunResponse response = workflowCommandService.startRun(apiKey.organizationId(), workflowId);
        return ResponseEntity.created(URI.create("/api/workflow-runs/" + response.id())).body(response);
    }
}
