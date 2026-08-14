package com.mihirgamre.taskforge.scheduler.task;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        classes = TaskClaimConcurrencyTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=true",
                "otel.sdk.disabled=true"
        }
)
class TaskClaimConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.4");

    @Autowired
    private TaskExecutionRepository repository;

    @Autowired
    private TaskClaimService claimService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void simultaneousClaimsCannotClaimSameTask() throws Exception {
        TaskExecution task = repository.save(TaskExecution.createNoOp(
                "tenant-a",
                "concurrency",
                Instant.parse("2026-08-14T10:00:00Z")
        ));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Optional<UUID>> claim = () -> {
            start.await();
            return claimService.claimNextPendingTask();
        };

        try {
            Future<Optional<UUID>> first = executor.submit(claim);
            Future<Optional<UUID>> second = executor.submit(claim);
            start.countDown();

            List<Optional<UUID>> results = List.of(first.get(), second.get());

            assertThat(results).contains(Optional.of(task.id()), Optional.empty());
            assertThat(results.stream().filter(Optional::isPresent).map(Optional::orElseThrow))
                    .containsExactly(task.id());
            TaskExecution persisted = repository.findById(task.id()).orElseThrow();
            assertThat(persisted.status()).isEqualTo(TaskStatus.DISPATCHED);
            assertThat(persisted.attemptCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("com.mihirgamre.taskforge.domain")
    @EnableJpaRepositories("com.mihirgamre.taskforge.domain")
    @Import(TaskClaimService.class)
    static class TestApplication {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-14T10:01:00Z"), ZoneOffset.UTC);
        }
    }
}
