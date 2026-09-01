package com.mihirgamre.taskforge.controlplane.approval;

import com.mihirgamre.taskforge.controlplane.auth.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
class ApprovalController {
    private final ApprovalService service;

    ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping
    List<ApprovalTaskResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listWaiting(user.organizationId());
    }

    @PostMapping("/{taskId}/approve")
    ApprovalTaskResponse approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID taskId) {
        user.requireWriteAccess();
        return service.approve(user.organizationId(), taskId);
    }

    @PostMapping("/{taskId}/reject")
    ApprovalTaskResponse reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID taskId,
            @RequestBody(required = false) RejectApprovalRequest request
    ) {
        user.requireWriteAccess();
        return service.reject(user.organizationId(), taskId, request == null ? null : request.reason());
    }
}
