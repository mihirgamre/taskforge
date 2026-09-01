package com.mihirgamre.taskforge.controlplane.automation;

import com.mihirgamre.taskforge.domain.automation.ApiKey;
import com.mihirgamre.taskforge.domain.automation.ApiKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiKeyService {
    private final ApiKeyRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    public ApiKeyService(ApiKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CreatedApiKeyResponse create(UUID organizationId, CreateApiKeyRequest request) {
        Instant now = Instant.now(clock);
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API key expiry must be in the future");
        }
        byte[] secret = new byte[32];
        secureRandom.nextBytes(secret);
        String rawKey = "tfk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String prefix = rawKey.substring(0, 12);
        ApiKey key = repository.save(new ApiKey(organizationId, request.name(), prefix, hash(rawKey), now, request.expiresAt()));
        return CreatedApiKeyResponse.from(key, rawKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(UUID organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    @Transactional
    public void revoke(UUID organizationId, UUID keyId) {
        ApiKey key = repository.findByIdAndOrganizationId(keyId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        key.revoke(Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public Optional<ApiKey> authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        return repository.findByKeyHash(hash(rawKey)).filter(key -> key.activeAt(now));
    }

    private String hash(String rawKey) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
