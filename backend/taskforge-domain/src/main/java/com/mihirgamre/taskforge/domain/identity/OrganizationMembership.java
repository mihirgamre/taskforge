package com.mihirgamre.taskforge.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_membership")
public class OrganizationMembership {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrganizationMembership() {
    }

    private OrganizationMembership(UUID organizationId, UUID userId, OrganizationRole role, Instant now) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.userId = userId;
        this.role = role;
        this.createdAt = now;
    }

    public static OrganizationMembership create(
            UUID organizationId,
            UUID userId,
            OrganizationRole role,
            Instant now
    ) {
        return new OrganizationMembership(organizationId, userId, role, now);
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID userId() {
        return userId;
    }

    public OrganizationRole role() {
        return role;
    }
}
