package com.mihirgamre.taskforge.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_account")
public class OrganizationAccount {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationAccount() {
    }

    private OrganizationAccount(String name, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static OrganizationAccount create(String name, Instant now) {
        return new OrganizationAccount(name, now);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }
}
