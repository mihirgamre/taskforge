package com.mihirgamre.taskforge.domain.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationAccountRepository extends JpaRepository<OrganizationAccount, UUID> {
}
