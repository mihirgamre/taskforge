package com.mihirgamre.taskforge.domain.workflow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class DagValidator {
    public DagValidationResult validate(DagDefinition definition) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> nodeCounts = new LinkedHashMap<>();
        for (String nodeKey : definition.nodeKeys()) {
            if (nodeKey == null || nodeKey.isBlank()) {
                errors.add("Node keys must not be blank");
            } else {
                nodeCounts.merge(nodeKey, 1, Integer::sum);
            }
        }
        if (nodeCounts.isEmpty()) {
            errors.add("Workflow must contain at least one node");
        }
        nodeCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .forEach(entry -> errors.add("Duplicate node key: " + entry.getKey()));

        Set<String> nodeKeys = nodeCounts.keySet();
        Set<String> seenEdges = new HashSet<>();
        Map<String, Set<String>> outgoing = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        nodeKeys.forEach(nodeKey -> {
            outgoing.put(nodeKey, new LinkedHashSet<>());
            indegree.put(nodeKey, 0);
        });

        for (DagEdge edge : definition.edges()) {
            String source = edge.sourceNodeKey();
            String target = edge.targetNodeKey();
            if (source == null || target == null || source.isBlank() || target.isBlank()) {
                errors.add("Edge endpoints must not be blank");
                continue;
            }
            if (source.equals(target)) {
                errors.add("Self edge is not allowed: " + source);
            }
            if (!nodeKeys.contains(source)) {
                errors.add("Missing source node: " + source);
            }
            if (!nodeKeys.contains(target)) {
                errors.add("Missing target node: " + target);
            }
            String edgeKey = source + "->" + target;
            if (!seenEdges.add(edgeKey)) {
                errors.add("Duplicate edge: " + edgeKey);
            }
            if (nodeKeys.contains(source) && nodeKeys.contains(target) && !source.equals(target)) {
                if (outgoing.get(source).add(target)) {
                    indegree.merge(target, 1, Integer::sum);
                }
            }
        }

        if (errors.stream().noneMatch(error -> error.startsWith("Duplicate node key"))) {
            detectCycle(nodeKeys, outgoing, indegree, errors);
        }
        return new DagValidationResult(List.copyOf(errors));
    }

    private void detectCycle(
            Set<String> nodeKeys,
            Map<String, Set<String>> outgoing,
            Map<String, Integer> indegree,
            List<String> errors
    ) {
        Queue<String> ready = new ArrayDeque<>();
        indegree.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(ready::add);
        int visited = 0;
        while (!ready.isEmpty()) {
            String node = ready.remove();
            visited++;
            outgoing.getOrDefault(node, Set.of()).stream().sorted().forEach(child -> {
                int next = indegree.computeIfPresent(child, (ignored, value) -> value - 1);
                if (next == 0) {
                    ready.add(child);
                }
            });
        }
        if (visited != nodeKeys.size()) {
            errors.add("Workflow graph contains a cycle");
        }
    }
}
