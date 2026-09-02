package com.mihirgamre.taskforge.controlplane.task;

import com.mihirgamre.taskforge.controlplane.ControlPlaneApplication;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        classes = ControlPlaneApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "otel.sdk.disabled=true",
                "taskforge.auth.jwt-secret=test-only-taskforge-jwt-secret-value",
                "taskforge.rate-limit.enabled=false"
        }
)
class TaskControllerSecurityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.4");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void rejectsUnauthenticatedTaskCreation() {
        RestClient client = RestClient.create("http://localhost:" + port);

        try {
            client.post()
                    .uri("/api/tasks/noop")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"description\":\"smoke\"}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(401));
            return;
        }
        throw new AssertionError("Expected unauthenticated request to fail");
    }

    @Test
    void exposesPrometheusMetricsWithoutAuthentication() {
        RestClient client = RestClient.create("http://localhost:" + port);

        var response = client.get()
                .uri("/actuator/prometheus")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).contains("# HELP");
    }

    @Test
    void createsAuthenticatedTaskForRegisteredOrganization() {
        RestClient client = RestClient.create("http://localhost:" + port);
        Map<?, ?> auth = client.post()
                .uri("/api/auth/register")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {"email":"owner@example.com","password":"correct horse battery","organizationName":"Example Org"}
                        """)
                .retrieve()
                .body(Map.class);

        var response = client
                .post()
                .uri("/api/tasks/noop")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.get("accessToken"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"description\":\"smoke\"}")
                .retrieve()
                .toEntity(TaskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TaskStatus.PENDING);
    }
}
