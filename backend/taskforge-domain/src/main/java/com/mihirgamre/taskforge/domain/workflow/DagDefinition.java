package com.mihirgamre.taskforge.domain.workflow;

import java.util.List;

public record DagDefinition(List<String> nodeKeys, List<DagEdge> edges) {
}
