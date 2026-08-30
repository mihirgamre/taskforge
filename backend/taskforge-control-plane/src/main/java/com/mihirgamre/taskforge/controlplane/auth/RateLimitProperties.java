package com.mihirgamre.taskforge.controlplane.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskforge.rate-limit")
public record RateLimitProperties(boolean enabled, int requests, Duration window) {
    public RateLimitProperties {
        if (requests <= 0) {
            requests = 120;
        }
        if (window == null) {
            window = Duration.ofMinutes(1);
        }
    }
}
