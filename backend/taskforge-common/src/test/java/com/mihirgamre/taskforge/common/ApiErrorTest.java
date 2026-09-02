package com.mihirgamre.taskforge.common;

import com.mihirgamre.taskforge.common.api.ApiError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    @Test
    void createsStandardError() {
        ApiError error = ApiError.of(404, "NOT_FOUND", "Missing");

        assertThat(error.status()).isEqualTo(404);
        assertThat(error.code()).isEqualTo("NOT_FOUND");
        assertThat(error.details()).isEmpty();
        assertThat(error.requestId()).isNull();
    }
}
