package com.mihirgamre.taskforge.controlplane.auth;

import com.mihirgamre.taskforge.domain.identity.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 120) String organizationName
) {
}

record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
}

record RefreshRequest(@NotBlank String refreshToken) {
}

record LogoutRequest(@NotBlank String refreshToken) {
}

record AuthResponse(String accessToken, String refreshToken, AuthUserResponse user, AuthOrganizationResponse organization) {
}

record AuthUserResponse(UUID id, String email) {
}

record AuthOrganizationResponse(UUID id, String name, OrganizationRole role) {
}

record MeResponse(UUID userId, String email, UUID organizationId, OrganizationRole role) {
    static MeResponse from(AuthenticatedUser user) {
        return new MeResponse(user.userId(), user.email(), user.organizationId(), user.role());
    }
}
