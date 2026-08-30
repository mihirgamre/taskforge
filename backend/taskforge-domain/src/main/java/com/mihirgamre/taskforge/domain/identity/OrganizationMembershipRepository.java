package com.mihirgamre.taskforge.domain.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    Optional<OrganizationMembership> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    Optional<OrganizationMembership> findFirstByUserIdOrderByCreatedAtAsc(UUID userId);
}
