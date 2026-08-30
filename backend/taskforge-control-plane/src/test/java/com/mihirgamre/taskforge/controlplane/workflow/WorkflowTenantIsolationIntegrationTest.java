package com.mihirgamre.taskforge.controlplane.workflow;

import com.mihirgamre.taskforge.controlplane.ControlPlaneApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
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
class WorkflowTenantIsolationIntegrationTest {

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
    void doesNotExposeWorkflowAcrossOrganizations() {
        RestClient client = RestClient.create("http://localhost:" + port);
        String orgAToken = (String) register(client, "workflow-a@example.com", "Org A").get("accessToken");
        String orgBToken = (String) register(client, "workflow-b@example.com", "Org B").get("accessToken");

        Map<?, ?> workflow = client.post()
                .uri("/api/workflows")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"isolated\",\"description\":\"tenant scoped\"}")
                .retrieve()
                .body(Map.class);

        try {
            client.get()
                    .uri("/api/workflows/{workflowId}", workflow.get("id"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + orgBToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
            return;
        }
        throw new AssertionError("Expected cross-organization workflow read to fail");
    }

    private Map<?, ?> register(RestClient client, String email, String organizationName) {
        return client.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email":"%s","password":"correct horse battery","organizationName":"%s"}
                        """.formatted(email, organizationName))
                .retrieve()
                .body(Map.class);
    }
}
