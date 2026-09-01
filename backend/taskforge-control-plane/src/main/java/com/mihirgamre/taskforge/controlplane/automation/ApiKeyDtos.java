package com.mihirgamre.taskforge.controlplane.automation;

import com.mihirgamre.taskforge.domain.automation.ApiKey;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

record CreateApiKeyRequest(@NotBlank String name, Instant expiresAt) {
}

record ApiKeyResponse(UUID id, String name, String keyPrefix, Instant createdAt, Instant expiresAt, Instant revokedAt) {
    static ApiKeyResponse from(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.id(),
                apiKey.name(),
                apiKey.keyPrefix(),
                apiKey.createdAt(),
                apiKey.expiresAt(),
                apiKey.revokedAt()
        );
    }
}

record CreatedApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        String apiKey,
        Instant createdAt,
        Instant expiresAt
) {
    static CreatedApiKeyResponse from(ApiKey key, String rawKey) {
        return new CreatedApiKeyResponse(key.id(), key.name(), key.keyPrefix(), rawKey, key.createdAt(), key.expiresAt());
    }
}
