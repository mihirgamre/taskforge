package com.mihirgamre.taskforge.controlplane.automation;

import com.mihirgamre.taskforge.domain.automation.ApiKey;
import com.mihirgamre.taskforge.domain.automation.ApiKeyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyServiceTest {
    private final ApiKeyRepository repository = mock(ApiKeyRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
    private final ApiKeyService service = new ApiKeyService(repository, clock);

    @Test
    void createsKeyAndReturnsRawSecretOnlyOnce() {
        UUID organizationId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatedApiKeyResponse response = service.create(organizationId, new CreateApiKeyRequest("deploy", null));

        assertThat(response.apiKey()).startsWith("tfk_");
        assertThat(response.keyPrefix()).isEqualTo(response.apiKey().substring(0, 12));
        assertThat(response.name()).isEqualTo("deploy");
    }

    @Test
    void authenticatesActiveStoredKeyByHash() {
        UUID organizationId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreatedApiKeyResponse created = service.create(organizationId, new CreateApiKeyRequest("deploy", null));
        when(repository.findByKeyHash(any())).thenAnswer(invocation -> {
            String hash = invocation.getArgument(0);
            return Optional.of(new ApiKey(organizationId, "deploy", created.keyPrefix(), hash, clock.instant(), null));
        });

        assertThat(service.authenticate(created.apiKey()))
                .isPresent()
                .get()
                .extracting(ApiKey::organizationId)
                .isEqualTo(organizationId);
    }

    @Test
    void revocationPreventsAuthentication() {
        UUID organizationId = UUID.randomUUID();
        ApiKey key = new ApiKey(organizationId, "deploy", "tfk_abc12345", "hash", clock.instant(), null);
        key.revoke(clock.instant());
        when(repository.findByKeyHash(any())).thenReturn(Optional.of(key));

        assertThat(service.authenticate("tfk_anything")).isEmpty();
    }

    @Test
    void listDoesNotExposeRawSecret() {
        UUID organizationId = UUID.randomUUID();
        ApiKey key = new ApiKey(organizationId, "deploy", "tfk_abc12345", "hash", clock.instant(), null);
        when(repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)).thenReturn(List.of(key));

        assertThat(service.list(organizationId)).singleElement()
                .satisfies(response -> {
                    assertThat(response.keyPrefix()).isEqualTo("tfk_abc12345");
                    assertThat(response.name()).isEqualTo("deploy");
                });
    }

    @Test
    void rejectsPastExpiry() {
        assertThatThrownBy(() -> service.create(
                UUID.randomUUID(),
                new CreateApiKeyRequest("deploy", Instant.parse("2026-09-01T09:59:00Z"))
        )).isInstanceOf(ResponseStatusException.class);
    }
}
