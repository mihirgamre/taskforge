package com.mihirgamre.taskforge.domain.automation;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleCronTest {
    @Test
    void computesNextMinuteForWildcardMinuteAndHour() {
        Instant next = SimpleCron.nextRun("* * * * *", "UTC", Instant.parse("2026-09-01T10:15:30Z"));

        assertThat(next).isEqualTo(Instant.parse("2026-09-01T10:16:00Z"));
    }

    @Test
    void computesNextSteppedMinuteInRequestedTimeZone() {
        Instant next = SimpleCron.nextRun("*/15 * * * *", "America/Phoenix", Instant.parse("2026-09-01T10:07:00Z"));

        assertThat(next).isEqualTo(Instant.parse("2026-09-01T10:15:00Z"));
    }

    @Test
    void computesFixedHourAndMinute() {
        Instant next = SimpleCron.nextRun("30 9 * * *", "UTC", Instant.parse("2026-09-01T10:00:00Z"));

        assertThat(next).isEqualTo(Instant.parse("2026-09-02T09:30:00Z"));
    }

    @Test
    void rejectsUnsupportedCronShape() {
        assertThatThrownBy(() -> SimpleCron.nextRun("0 9 * * 1", "UTC", Instant.parse("2026-09-01T10:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minute/hour cron");
    }

    @Test
    void rejectsInvalidTimeZone() {
        assertThatThrownBy(() -> SimpleCron.nextRun("* * * * *", "No/SuchZone", Instant.parse("2026-09-01T10:00:00Z")))
                .isInstanceOf(RuntimeException.class);
    }
}
