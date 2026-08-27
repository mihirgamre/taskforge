package com.mihirgamre.taskforge.domain.workflow;

import java.util.List;

public record DagValidationResult(List<String> errors) {
    public boolean valid() {
        return errors.isEmpty();
    }

    public void throwIfInvalid() {
        if (!valid()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }
}
