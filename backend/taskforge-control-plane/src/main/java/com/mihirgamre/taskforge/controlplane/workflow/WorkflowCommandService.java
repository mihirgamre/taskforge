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
    public WorkflowResponse create(UUID organizationId, CreateWorkflowRequest request) {
        Instant now = Instant.now(clock);
        Workflow workflow = workflowRepository.save(Workflow.create(organizationId, request.name(), request.description(), now));
        WorkflowVersion draft = versionRepository.save(WorkflowVersion.draft(workflow.id(), 1, now));
        return WorkflowResponse.from(workflow, draft);
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> list(UUID organizationId) {
        return workflowRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId).stream()
                .map(workflow -> WorkflowResponse.from(workflow, versionRepository
                        .findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(workflow.id(), WorkflowVersionStatus.DRAFT)
                        .orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowResponse get(UUID organizationId, UUID workflowId) {
        Workflow workflow = findWorkflow(organizationId, workflowId);
        WorkflowVersion draft = versionRepository
                .findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(workflowId, WorkflowVersionStatus.DRAFT)
                .orElse(null);
        return WorkflowResponse.from(workflow, draft);
    }

    @Transactional(readOnly = true)
    public WorkflowDraftResponse getDraft(UUID organizationId, UUID workflowId) {
        return draftResponse(findDraft(organizationId, workflowId));
    }

    @Transactional
    public WorkflowDraftResponse replaceDraft(UUID organizationId, UUID workflowId, UpdateDraftWorkflowRequest request) {
        WorkflowVersion draft = findOrCreateDraft(organizationId, workflowId);
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
    public WorkflowValidationResponse validate(UUID organizationId, UUID workflowId) {
        WorkflowVersion draft = findDraft(organizationId, workflowId);
        DagValidationResult result = validateVersion(draft.id());
        return new WorkflowValidationResponse(result.valid(), result.errors());
    }

    @Transactional
    public WorkflowDraftResponse publish(UUID organizationId, UUID workflowId) {
        WorkflowVersion draft = findDraft(organizationId, workflowId);
        validateVersion(draft.id()).throwIfInvalid();
        draft.publish(Instant.now(clock));
        return draftResponse(draft);
    }

    @Transactional
    public WorkflowRunResponse startRun(UUID organizationId, UUID workflowId) {
        findWorkflow(organizationId, workflowId);
        WorkflowVersion version = versionRepository
                .findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(workflowId, WorkflowVersionStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow has no published version"));
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowVersionId(version.id());
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowVersionId(version.id());
        Set<String> nonRootNodes = new HashSet<>();
        edges.forEach(edge -> nonRootNodes.add(edge.targetNodeKey()));

        Instant now = Instant.now(clock);
        WorkflowRun run = runRepository.save(WorkflowRun.start(workflowId, version.id(), organizationId, now));
        List<TaskExecution> tasks = nodes.stream()
                .map(node -> TaskExecution.createWorkflowTask(
                        organizationId,
                        run.id(),
                        node.nodeKey(),
                        node.name(),
                        node.type(),
                        node.configuration(),
                        nonRootNodes.contains(node.nodeKey()) ? TaskStatus.BLOCKED : TaskStatus.PENDING,
                        now
                ))
                .toList();
        taskRepository.saveAll(tasks);
        return WorkflowRunResponse.from(run);
    }

    @Transactional(readOnly = true)
    public WorkflowRunResponse getRun(UUID organizationId, UUID runId) {
        return WorkflowRunResponse.from(runRepository.findByIdAndOrganizationId(runId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found")));
    }

    @Transactional(readOnly = true)
    public List<WorkflowTaskResponse> getRunTasks(UUID organizationId, UUID runId) {
        if (!runRepository.existsByIdAndOrganizationId(runId, organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found");
        }
        return taskRepository.findByWorkflowRunIdAndOrganizationId(runId, organizationId).stream()
                .map(WorkflowTaskResponse::from)
                .toList();
    }

    private Workflow findWorkflow(UUID organizationId, UUID workflowId) {
        return workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
    }

    private WorkflowVersion findDraft(UUID organizationId, UUID workflowId) {
        findWorkflow(organizationId, workflowId);
        return versionRepository.findFirstByWorkflowIdAndStatusOrderByVersionNumberDesc(
                        workflowId,
                        WorkflowVersionStatus.DRAFT
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow has no editable draft"));
    }

    private WorkflowVersion findOrCreateDraft(UUID organizationId, UUID workflowId) {
        findWorkflow(organizationId, workflowId);
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
