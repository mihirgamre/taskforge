package com.mihirgamre.taskforge.controlplane.auth;

import com.mihirgamre.taskforge.domain.identity.OrganizationRole;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, UUID organizationId, OrganizationRole role) {
    public void requireWriteAccess() {
        if (!role.canWrite()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Write access is required"
            );
        }
    }
}
