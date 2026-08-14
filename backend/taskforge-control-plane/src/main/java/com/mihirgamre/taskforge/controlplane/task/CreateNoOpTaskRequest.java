package com.mihirgamre.taskforge.controlplane.task;

import jakarta.validation.constraints.Size;

public record CreateNoOpTaskRequest(
        @Size(max = 500) String description
) {
}
