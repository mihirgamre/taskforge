package com.mihirgamre.taskforge.controlplane.auth;

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
class AuthIntegrationTest {

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
    void registersLogsInAndRotatesRefreshToken() {
        RestClient client = RestClient.create("http://localhost:" + port);

        Map<?, ?> registered = register(client, "auth-owner@example.com", "Auth Org");
        assertThat(registered.get("accessToken")).isInstanceOf(String.class);
        assertThat(registered.get("refreshToken")).isInstanceOf(String.class);

        Map<?, ?> login = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email":"auth-owner@example.com","password":"correct horse battery"}
                        """)
                .retrieve()
                .body(Map.class);
        assertThat(login.get("accessToken")).isInstanceOf(String.class);

        String firstRefresh = (String) registered.get("refreshToken");
        Map<?, ?> refreshed = client.post()
                .uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"refreshToken\":\"" + firstRefresh + "\"}")
                .retrieve()
                .body(Map.class);
        assertThat(refreshed.get("refreshToken")).isNotEqualTo(firstRefresh);

        try {
            client.post()
                    .uri("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"refreshToken\":\"" + firstRefresh + "\"}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(401));
            return;
        }
        throw new AssertionError("Expected reused refresh token to fail");
    }

    @Test
    void returnsCurrentAuthenticatedUserContext() {
        RestClient client = RestClient.create("http://localhost:" + port);
        Map<?, ?> auth = register(client, "me-owner@example.com", "Me Org");

        Map<?, ?> me = client.get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.get("accessToken"))
                .retrieve()
                .body(Map.class);

        assertThat(me.get("email")).isEqualTo("me-owner@example.com");
        assertThat(me.get("role")).isEqualTo("OWNER");
        assertThat(me.get("organizationId")).isNotNull();
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
