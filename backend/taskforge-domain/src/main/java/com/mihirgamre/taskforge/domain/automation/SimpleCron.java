package com.mihirgamre.taskforge.domain.automation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class SimpleCron {
    private SimpleCron() {
    }

    public static Instant nextRun(String expression, String zoneId, Instant after) {
        String[] parts = expression == null ? new String[0] : expression.trim().split("\\s+");
        if (parts.length != 5 || !"*".equals(parts[2]) || !"*".equals(parts[3]) || !"*".equals(parts[4])) {
            throw new IllegalArgumentException("M5 schedules support five-field minute/hour cron expressions only");
        }
        ZoneId zone = ZoneId.of(zoneId);
        ZonedDateTime cursor = ZonedDateTime.ofInstant(after, zone).plusMinutes(1).withSecond(0).withNano(0);
        for (int i = 0; i < 1440; i++) {
            if (matches(parts[0], cursor.getMinute(), 0, 59) && matches(parts[1], cursor.getHour(), 0, 23)) {
                return cursor.toInstant();
            }
            cursor = cursor.plusMinutes(1);
        }
        throw new IllegalArgumentException("Cron expression did not produce a run within 24 hours");
    }

    private static boolean matches(String field, int value, int min, int max) {
        if ("*".equals(field)) {
            return true;
        }
        if (field.startsWith("*/")) {
            int step = Integer.parseInt(field.substring(2));
            if (step <= 0 || step > max) {
                throw new IllegalArgumentException("Invalid cron step");
            }
            return value % step == 0;
        }
        int fixed = Integer.parseInt(field);
        if (fixed < min || fixed > max) {
            throw new IllegalArgumentException("Cron value out of range");
        }
        return value == fixed;
    }
}
