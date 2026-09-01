package com.mihirgamre.taskforge.domain.automation;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowScheduleRepository extends JpaRepository<WorkflowSchedule, UUID> {
    List<WorkflowSchedule> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<WorkflowSchedule> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select schedule from WorkflowSchedule schedule
            where schedule.enabled = true
              and schedule.nextRunAt is not null
              and schedule.nextRunAt <= :now
            order by schedule.nextRunAt asc
            """)
    List<WorkflowSchedule> findDueSchedules(@Param("now") Instant now, Pageable pageable);
}
