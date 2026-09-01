package com.mihirgamre.taskforge.worker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerApplicationTest {

    @Test
    void applicationClassExists() {
        assertThat(WorkerApplication.class).isNotNull();
    }

    @Test
    void providesRestClientBuilderForHttpTasks() {
        assertThat(new WorkerApplication().restClientBuilder()).isNotNull();
    }
}
