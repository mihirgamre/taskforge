package com.mihirgamre.taskforge.controlplane.task;

import com.mihirgamre.taskforge.common.api.GlobalExceptionHandler;
import com.mihirgamre.taskforge.controlplane.auth.AuthenticatedUser;
import com.mihirgamre.taskforge.domain.identity.OrganizationRole;
import com.mihirgamre.taskforge.domain.task.TaskExecution;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerMvcTest {

    private final TaskCommandService service = mock(TaskCommandService.class);
    private MockMvc mockMvc;
    private final UUID organizationId = UUID.randomUUID();
    private final AuthenticatedUser user = new AuthenticatedUser(
            UUID.randomUUID(),
            "user@example.com",
            organizationId,
            OrganizationRole.MEMBER
    );

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TaskController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsNoOpTask() throws Exception {
        Instant now = Instant.parse("2026-08-14T10:00:00Z");
        TaskExecution execution = TaskExecution.createNoOp(organizationId, "smoke", now);
        when(service.createNoOp(organizationId, "smoke")).thenReturn(execution);

        mockMvc.perform(post("/api/tasks/noop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"smoke\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/" + execution.id()))
                .andExpect(jsonPath("$.id").value(execution.id().toString()))
                .andExpect(jsonPath("$.tenantId").value(organizationId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void retrievesExistingTask() throws Exception {
        Instant now = Instant.parse("2026-08-14T10:00:00Z");
        TaskExecution execution = TaskExecution.createNoOp(organizationId, "smoke", now);
        when(service.getForOrganization(execution.id(), organizationId)).thenReturn(execution);

        mockMvc.perform(get("/api/tasks/{id}", execution.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(execution.id().toString()))
                .andExpect(jsonPath("$.tenantId").value(organizationId.toString()));
    }

    @Test
    void returnsNotFoundForMissingTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(service.getForOrganization(taskId, organizationId))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Task not found"));

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/tasks/noop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{description:\"broken\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void returnsBadRequestForInvalidTaskId() throws Exception {
        mockMvc.perform(get("/api/tasks/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
