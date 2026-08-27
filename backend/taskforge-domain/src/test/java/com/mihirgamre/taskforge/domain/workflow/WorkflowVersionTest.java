package com.mihirgamre.taskforge.domain.workflow;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowVersionTest {
    @Test
    void publishedVersionIsImmutable() {
        WorkflowVersion version = WorkflowVersion.draft(
                UUID.randomUUID(),
                1,
                Instant.parse("2026-08-26T10:00:00Z")
        );
        version.publish(Instant.parse("2026-08-26T10:01:00Z"));

        assertThatThrownBy(version::requireDraft)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Published workflow versions are immutable");
    }
}
