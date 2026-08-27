package com.mihirgamre.taskforge.domain.workflow;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DagValidatorTest {
    private final DagValidator validator = new DagValidator();

    @Test
    void acceptsLinearDag() {
        assertThat(validate(List.of("A", "B", "C"), List.of(edge("A", "B"), edge("B", "C")))).isEmpty();
    }

    @Test
    void acceptsFanOutDag() {
        assertThat(validate(List.of("A", "B", "C"), List.of(edge("A", "B"), edge("A", "C")))).isEmpty();
    }

    @Test
    void acceptsFanInDag() {
        assertThat(validate(List.of("A", "B", "C"), List.of(edge("A", "C"), edge("B", "C")))).isEmpty();
    }

    @Test
    void acceptsMultipleRoots() {
        assertThat(validate(List.of("A", "B"), List.of())).isEmpty();
    }

    @Test
    void rejectsCycles() {
        assertThat(validate(List.of("A", "B", "C"), List.of(edge("A", "B"), edge("B", "C"), edge("C", "A"))))
                .contains("Workflow graph contains a cycle");
    }

    @Test
    void rejectsSelfEdges() {
        assertThat(validate(List.of("A"), List.of(edge("A", "A")))).contains("Self edge is not allowed: A");
    }

    @Test
    void rejectsDuplicateEdges() {
        assertThat(validate(List.of("A", "B"), List.of(edge("A", "B"), edge("A", "B"))))
                .contains("Duplicate edge: A->B");
    }

    @Test
    void rejectsMissingNodeReferences() {
        assertThat(validate(List.of("A"), List.of(edge("A", "B"), edge("C", "A"))))
                .contains("Missing target node: B", "Missing source node: C");
    }

    @Test
    void rejectsDuplicateNodeKeys() {
        assertThat(validate(List.of("A", "A"), List.of())).contains("Duplicate node key: A");
    }

    @Test
    void rejectsEmptyWorkflow() {
        assertThat(validate(List.of(), List.of())).contains("Workflow must contain at least one node");
    }

    private List<String> validate(List<String> nodes, List<DagEdge> edges) {
        return validator.validate(new DagDefinition(nodes, edges)).errors();
    }

    private DagEdge edge(String source, String target) {
        return new DagEdge(source, target);
    }
}
