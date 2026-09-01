package com.mihirgamre.taskforge.domain.automation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<ApiKey> findByKeyHash(String keyHash);

    Optional<ApiKey> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
