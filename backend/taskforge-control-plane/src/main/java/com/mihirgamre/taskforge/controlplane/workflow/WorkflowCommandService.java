package com.mihirgamre.taskforge.controlplane.workflow;

import com.mihirgamre.taskforge.domain.task.TaskExecution;
import com.mihirgamre.taskforge.domain.task.TaskExecutionRepository;
import com.mihirgamre.taskforge.domain.task.TaskStatus;
import com.mihirgamre.taskforge.domain.workflow.DagDefinition;
import com.mihirgamre.taskforge.domain.workflow.DagEdge;
import com.mihirgamre.taskforge.domain.workflow.DagValidationResult;
import com.mihirgamre.taskforge.domain.workflow.DagValidator;
import com.mihirgamre.taskforge.domain.workflow.Workflow;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdge;
import com.mihirgamre.taskforge.domain.workflow.WorkflowEdgeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowNode;
import com.mihirgamre.taskforge.domain.workflow.WorkflowNodeRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRun;
import com.mihirgamre.taskforge.domain.workflow.WorkflowRunRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersion;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionRepository;
import com.mihirgamre.taskforge.domain.workflow.WorkflowVersionStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowCommandService {
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowRunRepository runRepository;
    private final TaskExecutionRepository taskRepository;
    private final Clock clock;
    private final DagValidator dagValidator = new DagValidator();

    public WorkflowCommandService(
            WorkflowRepository workflowRepository,
            WorkflowVersionRepository versionRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowEdgeRepository edgeRepository,
            WorkflowRunRepository runRepository,
            TaskExecutionRepository taskRepository,
            Clock clock
    ) {
        this.workflowRepository = workflowRepository;
        this.versionRepository = versionRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional
    public WorkflowResponse create(CreateWorkflowRequest request) {
        Instant now = Instant.now(clock);
        Workflow workflow = workflowRepository.save(Workflow.create(request.name(), request.description(), now));
        WorkflowVersion draft = versionRepository.save(WorkflowVersion.draft(workflow.id(), 1, now));
        return WorkflowResponse.from(workflow, draft);
    }

    @Transactional(readOnly = true)
    public WorkflowResponse get(UUID workflowId) {
        Workflow workflow = findWorkflow(workflowId);
        WorkflowVersion draft = versionRepository
                .findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(workflowId, WorkflowVersionStatus.DRAFT)
                .orElse(null);
        return WorkflowResponse.from(workflow, draft);
    }

    @Transactional
    public WorkflowDraftResponse replaceDraft(UUID workflowId, UpdateDraftWorkflowRequest request) {
        WorkflowVersion draft = findOrCreateDraft(workflowId);
        draft.requireDraft();
        DagValidationResult requestValidation = dagValidator.validate(new DagDefinition(
                request.nodes().stream().map(WorkflowNodeRequest::nodeKey).toList(),
                edgesOrEmpty(request).stream()
                        .map(edge -> new DagEdge(edge.sourceNodeKey(), edge.targetNodeKey()))
                        .toList()
        ));
        requestValidation.throwIfInvalid();
        edgeRepository.deleteByWorkflowVersionId(draft.id());
        nodeRepository.deleteByWorkflowVersionId(draft.id());
        Instant now = Instant.now(clock);
        List<WorkflowNode> nodes = request.nodes().stream()
                .map(node -> new WorkflowNode(
                        draft.id(),
                        node.nodeKey(),
                        node.type(),
                        node.name(),
                        node.configuration(),
                        now
                ))
                .toList();
        nodeRepository.saveAll(nodes);
        List<WorkflowEdge> edges = edgesOrEmpty(request).stream()
                .map(edge -> new WorkflowEdge(draft.id(), edge.sourceNodeKey(), edge.targetNodeKey()))
                .toList();
        edgeRepository.saveAll(edges);
        return draftResponse(draft);
    }

    @Transactional(readOnly = true)
    public WorkflowValidationResponse validate(UUID workflowId) {
        WorkflowVersion draft = findDraft(workflowId);
        DagValidationResult result = validateVersion(draft.id());
        return new WorkflowValidationResponse(result.valid(), result.errors());
    }

    @Transactional
    public WorkflowDraftResponse publish(UUID workflowId) {
        WorkflowVersion draft = findDraft(workflowId);
        validateVersion(draft.id()).throwIfInvalid();
        draft.publish(Instant.now(clock));
        return draftResponse(draft);
    }

    @Transactional
    public WorkflowRunResponse startRun(UUID workflowId) {
        WorkflowVersion version = versionRepository
                .findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(workflowId, WorkflowVersionStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow has no published version"));
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowVersionId(version.id());
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowVersionId(version.id());
        Set<String> nonRootNodes = new HashSet<>();
        edges.forEach(edge -> nonRootNodes.add(edge.targetNodeKey()));

        Instant now = Instant.now(clock);
        WorkflowRun run = runRepository.save(WorkflowRun.start(workflowId, version.id(), now));
        List<TaskExecution> tasks = nodes.stream()
                .map(node -> TaskExecution.createWorkflowNoOp(
                        run.id(),
                        node.nodeKey(),
                        node.name(),
                        nonRootNodes.contains(node.nodeKey()) ? TaskStatus.BLOCKED : TaskStatus.PENDING,
                        now
                ))
                .toList();
        taskRepository.saveAll(tasks);
        return WorkflowRunResponse.from(run);
    }

    @Transactional(readOnly = true)
    public WorkflowRunResponse getRun(UUID runId) {
        return WorkflowRunResponse.from(runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found")));
    }

    @Transactional(readOnly = true)
    public List<WorkflowTaskResponse> getRunTasks(UUID runId) {
        if (!runRepository.existsById(runId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found");
        }
        return taskRepository.findByWorkflowRunId(runId).stream()
                .map(WorkflowTaskResponse::from)
                .toList();
    }

    private Workflow findWorkflow(UUID workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
    }

    private WorkflowVersion findDraft(UUID workflowId) {
        findWorkflow(workflowId);
        return versionRepository.findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(
                        workflowId,
                        WorkflowVersionStatus.DRAFT
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow has no editable draft"));
    }

    private WorkflowVersion findOrCreateDraft(UUID workflowId) {
        findWorkflow(workflowId);
        return versionRepository.findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(
                        workflowId,
                        WorkflowVersionStatus.DRAFT
                )
                .orElseGet(() -> {
                    int nextVersion = versionRepository.findFirstByWorkflowIdOrderByVersionNumberDesc(workflowId)
                            .map(version -> version.versionNumber() + 1)
                            .orElse(1);
                    return versionRepository.save(WorkflowVersion.draft(workflowId, nextVersion, Instant.now(clock)));
                });
    }

    private DagValidationResult validateVersion(UUID versionId) {
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowVersionId(versionId);
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowVersionId(versionId);
        return dagValidator.validate(new DagDefinition(
                nodes.stream().map(WorkflowNode::nodeKey).toList(),
                edges.stream().map(edge -> new DagEdge(edge.sourceNodeKey(), edge.targetNodeKey())).toList()
        ));
    }

    private WorkflowDraftResponse draftResponse(WorkflowVersion draft) {
        return new WorkflowDraftResponse(
                draft.workflowId(),
                draft.id(),
                draft.versionNumber(),
                draft.status(),
                nodeRepository.findByWorkflowVersionId(draft.id()).stream().map(WorkflowNodeResponse::from).toList(),
                edgeRepository.findByWorkflowVersionId(draft.id()).stream().map(WorkflowEdgeResponse::from).toList()
        );
    }

    private List<WorkflowEdgeRequest> edgesOrEmpty(UpdateDraftWorkflowRequest request) {
        return request.edges() == null ? List.of() : request.edges();
    }
}
