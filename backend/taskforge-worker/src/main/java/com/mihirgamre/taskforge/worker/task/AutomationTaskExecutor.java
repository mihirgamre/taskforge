package com.mihirgamre.taskforge.worker.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskType;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AutomationTaskExecutor {
    private final TaskExecutionRepository repository;
    private final TaskCompletionService completionService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AutomationTaskExecutor(
            TaskExecutionRepository repository,
            TaskCompletionService completionService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.repository = repository;
        this.completionService = completionService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public boolean execute(UUID taskId, String workerId) {
        Optional<UUID> leaseToken = completionService.acquireLease(taskId, workerId, Duration.ofSeconds(30));
        if (leaseToken.isEmpty()) {
            return false;
        }
        TaskExecution task = repository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        try {
            String result = executeTask(task);
            if (task.taskType() == TaskType.APPROVAL) {
                return completionService.waitForApproval(taskId, leaseToken.get(), result);
            }
            return completionService.complete(taskId, leaseToken.get(), result);
        } catch (RuntimeException exception) {
            return completionService.failWithRetry(taskId, leaseToken.get(), exception.getMessage());
        }
    }

    private String executeTask(TaskExecution task) {
        return switch (task.taskType()) {
            case NO_OP -> "{\"status\":\"noop-complete\"}";
            case TRANSFORM -> executeTransform(task.taskConfiguration());
            case NOTIFICATION -> executeNotification(task.taskConfiguration());
            case APPROVAL -> executeApproval(task.taskConfiguration());
            case HTTP -> executeHttp(task.taskConfiguration());
        };
    }

    private String executeTransform(String configuration) {
        JsonNode config = readConfig(configuration);
        JsonNode value = config.path("value");
        if (value.isMissingNode()) {
            throw new IllegalArgumentException("TRANSFORM task requires configuration.value");
        }
        return "{\"transformed\":" + value + "}";
    }

    private String executeNotification(String configuration) {
        JsonNode config = readConfig(configuration);
        String channel = text(config, "channel", "log");
        String message = text(config, "message", "");
        return "{\"channel\":\"" + escape(channel) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private String executeApproval(String configuration) {
        JsonNode config = readConfig(configuration);
        String prompt = text(config, "prompt", "Approval required");
        return "{\"prompt\":\"" + escape(prompt) + "\"}";
    }

    private String executeHttp(String configuration) {
        JsonNode config = readConfig(configuration);
        String method = text(config, "method", "GET").toUpperCase(Locale.ROOT);
        String url = text(config, "url", null);
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("HTTP task requires configuration.url");
        }
        URI uri = URI.create(url);
        validateHttpTarget(uri);
        if (!method.equals("GET") && !method.equals("POST")) {
            throw new IllegalArgumentException("HTTP task supports only GET and POST in M5");
        }
        ResponseEntity<String> response = restClient.method(HttpMethod.valueOf(method))
                .uri(uri)
                .retrieve()
                .toEntity(String.class);
        return "{\"statusCode\":" + response.getStatusCode().value() + "}";
    }

    private JsonNode readConfig(String configuration) {
        try {
            return objectMapper.readTree(configuration == null || configuration.isBlank() ? "{}" : configuration);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Task configuration must be valid JSON", exception);
        }
    }

    private void validateHttpTarget(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("HTTP task URL must use http or https");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("localhost") || normalizedHost.equals("127.0.0.1") || normalizedHost.equals("0.0.0.0")
                || normalizedHost.equals("::1") || normalizedHost.endsWith(".local") || isPrivateIpv4Literal(normalizedHost)) {
            throw new IllegalArgumentException("HTTP task URL targets a blocked host");
        }
    }

    private boolean isPrivateIpv4Literal(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        int[] octets = new int[4];
        for (int i = 0; i < parts.length; i++) {
            try {
                octets[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException exception) {
                return false;
            }
            if (octets[i] < 0 || octets[i] > 255) {
                return false;
            }
        }
        return octets[0] == 10
                || octets[0] == 127
                || octets[0] == 0
                || (octets[0] == 169 && octets[1] == 254)
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168);
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : fallback;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
