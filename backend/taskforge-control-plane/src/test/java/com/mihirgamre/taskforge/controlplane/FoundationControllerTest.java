package com.mihirgamre.taskforge.controlplane;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoundationControllerTest {

    @Test
    void exposesFoundationServiceInfo() {
        FoundationController controller = new FoundationController();

        assertThat(controller.serviceInfo().service()).isEqualTo("taskforge-control-plane");
        assertThat(controller.serviceInfo().version()).isEqualTo("0.1.0-SNAPSHOT");
    }
}
