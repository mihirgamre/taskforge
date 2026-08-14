package com.mihirgamre.taskforge.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceBoundaryTest {

    @Test
    void definesFoundationServiceBoundaries() {
        assertThat(ServiceBoundary.values())
                .containsExactly(ServiceBoundary.CONTROL_PLANE, ServiceBoundary.SCHEDULER, ServiceBoundary.WORKER);
    }
}

