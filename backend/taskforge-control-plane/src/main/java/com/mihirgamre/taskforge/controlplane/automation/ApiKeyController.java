package com.mihirgamre.taskforge.controlplane.automation;

import com.mihirgamre.taskforge.controlplane.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/api-keys")
class ApiKeyController {
    private final ApiKeyService service;

    ApiKeyController(ApiKeyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreatedApiKeyResponse create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateApiKeyRequest request) {
        user.requireWriteAccess();
        return service.create(user.organizationId(), request);
    }

    @GetMapping
    List<ApiKeyResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(user.organizationId());
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID keyId) {
        user.requireWriteAccess();
        service.revoke(user.organizationId(), keyId);
    }
}
