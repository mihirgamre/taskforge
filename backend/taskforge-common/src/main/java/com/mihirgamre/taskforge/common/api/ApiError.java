package com.mihirgamre.taskforge.common.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<String> details,
        String requestId
) {
    public static ApiError of(int status, String code, String message) {
        return new ApiError(Instant.now(), status, code, message, List.of(), null);
    }

    public static ApiError of(int status, String code, String message, String requestId) {
        return new ApiError(Instant.now(), status, code, message, List.of(), requestId);
    }
}
