package com.mihirgamre.taskforge.controlplane.auth;

import com.mihirgamre.taskforge.domain.identity.OrganizationRole;
import java.time.Instant;
import java.util.UUID;

record JwtClaims(UUID userId, String email, UUID organizationId, OrganizationRole role, Instant expiresAt) {
}
