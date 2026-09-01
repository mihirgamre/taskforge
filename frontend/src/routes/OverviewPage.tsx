import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { clearSession } from '../api/session';
import {
  approveTask,
  createWorkflow,
  getWorkflowDraft,
  getWorkflowRun,
  getWorkflowRunTasks,
  listApprovals,
  listWorkflows,
  login,
  publishWorkflow,
  register,
  rejectTask,
  restoreSession,
  saveWorkflowDraft,
  startWorkflowRun,
  validateWorkflow,
  type ApprovalTask,
  type AuthResponse,
  type WorkflowEdge,
  type WorkflowNode,
  type WorkflowRun,
  type WorkflowSummary,
  type WorkflowTask,
} from '../api/client';

const authSchema = z.object({
  email: z.string().email(),
  password: z.string().min(12),
  organizationName: z.string().min(1).max(120),
});

const workflowSchema = z.object({
  name: z.string().min(1).max(120),
  description: z.string().max(500),
});

const nodeSchema = z.object({
  nodeKey: z.string().min(1).max(64).regex(/^[A-Za-z0-9_-]+$/),
  type: z.enum(['NO_OP', 'HTTP', 'TRANSFORM', 'APPROVAL', 'NOTIFICATION']),
  name: z.string().min(1).max(120),
  configuration: z.string(),
});

const edgeSchema = z.object({
  sourceNodeKey: z.string().min(1),
  targetNodeKey: z.string().min(1),
});

type AuthForm = z.infer<typeof authSchema>;
type WorkflowForm = z.infer<typeof workflowSchema>;
type NodeForm = z.infer<typeof nodeSchema>;
type EdgeForm = z.infer<typeof edgeSchema>;

const initialNodes: WorkflowNode[] = [
  { nodeKey: 'A', type: 'NO_OP', name: 'Intake', configuration: '{}' },
  { nodeKey: 'B', type: 'NO_OP', name: 'Process', configuration: '{}' },
  { nodeKey: 'C', type: 'NO_OP', name: 'Complete', configuration: '{}' },
];

const initialEdges: WorkflowEdge[] = [
  { sourceNodeKey: 'A', targetNodeKey: 'B' },
  { sourceNodeKey: 'B', targetNodeKey: 'C' },
];

export function OverviewPage() {
  const [auth, setAuth] = useState<AuthResponse | null>(null);

  useEffect(() => {
    let mounted = true;
    void restoreSession().then((session) => {
      if (mounted && session) {
        setAuth(session);
      }
    }).catch(() => undefined);
    return () => {
      mounted = false;
    };
  }, []);

  if (!auth) {
    return <AuthPanel onAuthenticated={setAuth} />;
  }

  return (
    <WorkflowConsole
      auth={auth}
      onSignOut={() => {
        clearSession();
        setAuth(null);
      }}
    />
  );
}

function AuthPanel({ onAuthenticated }: { onAuthenticated: (auth: AuthResponse) => void }) {
  const [mode, setMode] = useState<'register' | 'login'>('register');
  const form = useForm<AuthForm>({
    resolver: zodResolver(authSchema),
    defaultValues: {
      email: '',
      password: '',
      organizationName: 'TaskForge Lab',
    },
  });
  const mutation = useMutation({
    mutationFn: (input: AuthForm) =>
      mode === 'register'
        ? register(input)
        : login({ email: input.email, password: input.password }),
    onSuccess: onAuthenticated,
  });

  return (
    <main className="mx-auto grid max-w-7xl gap-8 px-6 py-8 lg:grid-cols-[1fr_1.2fr]">
      <section className="flex min-h-[520px] flex-col justify-between rounded-md border border-[#d6dee3] bg-white p-6">
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-[#4e6b74]">M4 Console</p>
          <h2 className="mt-2 text-3xl font-semibold">Build and run authenticated workflow DAGs.</h2>
          <p className="mt-4 max-w-xl text-[#52606a]">
            Sign in to create editable workflow drafts, validate dependencies, publish immutable versions, and track run state.
          </p>
        </div>
        <div className="grid gap-3 text-sm text-[#52606a] sm:grid-cols-3">
          <Metric label="Boundary" value="Org scoped" />
          <Metric label="Task types" value="M5 enabled" />
          <Metric label="Updates" value="Polling" />
        </div>
      </section>
      <section className="rounded-md border border-[#d6dee3] bg-white p-6">
        <div className="inline-flex rounded-md border border-[#c8d4db] p-1">
          <button className={modeButton(mode === 'register')} type="button" onClick={() => setMode('register')}>
            Register
          </button>
          <button className={modeButton(mode === 'login')} type="button" onClick={() => setMode('login')}>
            Sign in
          </button>
        </div>
        <form
          className="mt-6 grid gap-4"
          onSubmit={(event) => {
            void form.handleSubmit((values) => mutation.mutate(values))(event);
          }}
        >
          <Field label="Email" error={form.formState.errors.email?.message}>
            <input className={inputClass} type="email" autoComplete="email" {...form.register('email')} />
          </Field>
          <Field label="Password" error={form.formState.errors.password?.message}>
            <input
              className={inputClass}
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              {...form.register('password')}
            />
          </Field>
          {mode === 'register' ? (
            <Field label="Organization" error={form.formState.errors.organizationName?.message}>
              <input className={inputClass} {...form.register('organizationName')} />
            </Field>
          ) : null}
          {mutation.error ? <p className="text-sm font-medium text-[#a33a2a]">{mutation.error.message}</p> : null}
          <button className={primaryButton} type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Working...' : mode === 'register' ? 'Create workspace' : 'Sign in'}
          </button>
        </form>
      </section>
    </main>
  );
}

function WorkflowConsole({ auth, onSignOut }: { auth: AuthResponse; onSignOut: () => void }) {
  const queryClient = useQueryClient();
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string | null>(null);
  const [activeRunId, setActiveRunId] = useState<string | null>(null);
  const [activeRunEdges, setActiveRunEdges] = useState<WorkflowEdge[]>([]);
  const workflowsQuery = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows });

  useEffect(() => {
    if (!selectedWorkflowId && workflowsQuery.data?.[0]) {
      setSelectedWorkflowId(workflowsQuery.data[0].id);
    }
  }, [selectedWorkflowId, workflowsQuery.data]);

  return (
    <main className="mx-auto grid max-w-7xl gap-6 px-6 py-8 xl:grid-cols-[320px_1fr]">
      <aside className="space-y-4">
        <section className="rounded-md border border-[#d6dee3] bg-white p-4">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-sm font-medium text-[#52606a]">{auth.organization.name}</p>
              <p className="mt-1 text-lg font-semibold">{auth.user.email}</p>
            </div>
            <button className={ghostButton} type="button" onClick={onSignOut}>
              Sign out
            </button>
          </div>
        </section>
        <CreateWorkflowPanel
          onCreated={(workflow) => {
            queryClient.setQueryData<WorkflowSummary[]>(['workflows'], (current) => [workflow, ...(current ?? [])]);
            setSelectedWorkflowId(workflow.id);
            setActiveRunId(null);
            setActiveRunEdges([]);
          }}
        />
        <section className="rounded-md border border-[#d6dee3] bg-white">
          <div className="border-b border-[#d6dee3] px-4 py-3">
            <h2 className="font-semibold">Workflows</h2>
          </div>
          <div className="max-h-[420px] overflow-auto p-2">
            {workflowsQuery.isLoading ? <p className="p-3 text-sm text-[#52606a]">Loading workflows</p> : null}
            {workflowsQuery.data?.map((workflow) => (
              <button
                className={`w-full rounded-md px-3 py-3 text-left text-sm ${
                  selectedWorkflowId === workflow.id ? 'bg-[#e7f3f2] text-[#0a5962]' : 'hover:bg-[#f0f4f5]'
                }`}
                key={workflow.id}
                type="button"
                onClick={() => {
                  setSelectedWorkflowId(workflow.id);
                  setActiveRunId(null);
                  setActiveRunEdges([]);
                }}
              >
                <span className="block font-medium">{workflow.name}</span>
                <span className="mt-1 block text-xs text-[#52606a]">
                  Draft v{workflow.draftVersionNumber ?? '-'} - {workflow.status}
                </span>
              </button>
            ))}
          </div>
        </section>
      </aside>
      {selectedWorkflowId ? (
        <WorkflowWorkspace
          workflowId={selectedWorkflowId}
          activeRunEdges={activeRunEdges}
          activeRunId={activeRunId}
          onRunStarted={(runId, runEdges) => {
            setActiveRunId(runId);
            setActiveRunEdges(runEdges);
          }}
        />
      ) : (
        <section className="rounded-md border border-[#d6dee3] bg-white p-6">
          <h2 className="text-xl font-semibold">No workflow selected</h2>
          <p className="mt-2 text-[#52606a]">Create a workflow to open the builder.</p>
        </section>
      )}
    </main>
  );
}

function CreateWorkflowPanel({ onCreated }: { onCreated: (workflow: WorkflowSummary) => void }) {
  const form = useForm<WorkflowForm>({
    resolver: zodResolver(workflowSchema),
    defaultValues: { name: '', description: '' },
  });
  const mutation = useMutation({
    mutationFn: createWorkflow,
    onSuccess: (workflow) => {
      onCreated(workflow);
      form.reset();
    },
  });

  return (
    <section className="rounded-md border border-[#d6dee3] bg-white p-4">
      <h2 className="font-semibold">New workflow</h2>
      <form
        className="mt-4 grid gap-3"
        onSubmit={(event) => {
          void form.handleSubmit((values) => mutation.mutate(values))(event);
        }}
      >
        <input aria-label="Workflow name" className={inputClass} placeholder="Name" {...form.register('name')} />
        <textarea
          aria-label="Workflow description"
          className={`${inputClass} min-h-20 resize-y`}
          placeholder="Description"
          {...form.register('description')}
        />
        {mutation.error ? <p className="text-sm text-[#a33a2a]">{mutation.error.message}</p> : null}
        <button className={primaryButton} type="submit" disabled={mutation.isPending}>
          Create
        </button>
      </form>
    </section>
  );
}

function WorkflowWorkspace({
  workflowId,
  activeRunEdges,
  activeRunId,
  onRunStarted,
}: {
  workflowId: string;
  activeRunEdges: WorkflowEdge[];
  activeRunId: string | null;
  onRunStarted: (runId: string, runEdges: WorkflowEdge[]) => void;
}) {
  const queryClient = useQueryClient();
  const draftQuery = useQuery({ queryKey: ['workflow-draft', workflowId], queryFn: () => getWorkflowDraft(workflowId) });
  const [nodes, setNodes] = useState<WorkflowNode[]>(initialNodes);
  const [edges, setEdges] = useState<WorkflowEdge[]>(initialEdges);
  const [validation, setValidation] = useState<string[]>([]);

  useEffect(() => {
    if (draftQuery.data) {
      setNodes(draftQuery.data.nodes.length ? draftQuery.data.nodes : initialNodes);
      setEdges(draftQuery.data.nodes.length ? draftQuery.data.edges : initialEdges);
      setValidation([]);
    }
  }, [draftQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () => saveWorkflowDraft(workflowId, { nodes, edges }),
    onSuccess: (draft) => {
      queryClient.setQueryData(['workflow-draft', workflowId], draft);
      setValidation([]);
    },
  });
  const validateMutation = useMutation({
    mutationFn: async () => {
      await saveWorkflowDraft(workflowId, { nodes, edges });
      return validateWorkflow(workflowId);
    },
    onSuccess: (result) => setValidation(result.valid ? [] : result.errors),
  });
  const publishMutation = useMutation({
    mutationFn: async () => {
      await saveWorkflowDraft(workflowId, { nodes, edges });
      return publishWorkflow(workflowId);
    },
    onSuccess: (draft) => {
      queryClient.setQueryData(['workflow-draft', workflowId], draft);
      void queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });
  const runMutation = useMutation({
    mutationFn: () => startWorkflowRun(workflowId),
    onSuccess: (run) => onRunStarted(run.id, edges),
  });

  return (
    <div className="grid gap-6">
      <section className="rounded-md border border-[#d6dee3] bg-white">
        <div className="flex flex-col gap-3 border-b border-[#d6dee3] px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-xl font-semibold">Workflow builder</h2>
            <p className="text-sm text-[#52606a]">
              Draft v{draftQuery.data?.versionNumber ?? '-'} - {draftQuery.data?.status ?? 'loading'}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button className={secondaryButton} type="button" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
              Save
            </button>
            <button className={secondaryButton} type="button" onClick={() => validateMutation.mutate()} disabled={validateMutation.isPending}>
              Validate
            </button>
            <button className={secondaryButton} type="button" onClick={() => publishMutation.mutate()} disabled={publishMutation.isPending}>
              Publish
            </button>
            <button className={primaryButton} type="button" onClick={() => runMutation.mutate()} disabled={runMutation.isPending}>
              Run
            </button>
          </div>
        </div>
        <div className="grid gap-5 p-5 lg:grid-cols-[1fr_340px]">
          <GraphView nodes={nodes} edges={edges} />
          <DraftEditor nodes={nodes} edges={edges} onNodesChange={setNodes} onEdgesChange={setEdges} />
        </div>
        <ActionState
          errors={[saveMutation.error, validateMutation.error, publishMutation.error, runMutation.error]
            .filter((error): error is Error => error instanceof Error)
            .map((error) => error.message)
            .concat(validation)}
        />
      </section>
      <RunPanel runId={activeRunId} edges={activeRunEdges} />
      <ApprovalPanel runId={activeRunId} />
    </div>
  );
}

function DraftEditor({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
}: {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  onNodesChange: (nodes: WorkflowNode[]) => void;
  onEdgesChange: (edges: WorkflowEdge[]) => void;
}) {
  const nodeForm = useForm<NodeForm>({
    resolver: zodResolver(nodeSchema),
    defaultValues: { nodeKey: '', type: 'NO_OP', name: '', configuration: '{}' },
  });
  const edgeForm = useForm<EdgeForm>({
    resolver: zodResolver(edgeSchema),
    defaultValues: { sourceNodeKey: '', targetNodeKey: '' },
  });

  return (
    <div className="grid gap-4">
      <form
        className="grid gap-3 rounded-md border border-[#d6dee3] p-4"
        onSubmit={(event) => {
          void nodeForm.handleSubmit((value) => {
            const nextNode = { ...value };
            onNodesChange([...nodes.filter((node) => node.nodeKey !== value.nodeKey), nextNode]);
            nodeForm.reset({ nodeKey: '', type: 'NO_OP', name: '', configuration: '{}' });
          })(event);
        }}
      >
        <h3 className="font-semibold">Nodes</h3>
        <input aria-label="Node key" className={inputClass} placeholder="Key" {...nodeForm.register('nodeKey')} />
        <select aria-label="Node type" className={inputClass} {...nodeForm.register('type')}>
          <option value="NO_OP">No-op</option>
          <option value="TRANSFORM">Transform</option>
          <option value="APPROVAL">Approval</option>
          <option value="NOTIFICATION">Notification</option>
          <option value="HTTP">HTTP</option>
        </select>
        <input aria-label="Node name" className={inputClass} placeholder="Name" {...nodeForm.register('name')} />
        <textarea
          aria-label="Node configuration JSON"
          className={`${inputClass} min-h-16`}
          placeholder="Configuration JSON"
          {...nodeForm.register('configuration')}
        />
        <button className={secondaryButton} type="submit">
          Add node
        </button>
        <div className="grid gap-2">
          {nodes.map((node) => (
            <div className="flex items-center justify-between rounded-md bg-[#f0f4f5] px-3 py-2 text-sm" key={node.nodeKey}>
              <span>
                <strong>{node.nodeKey}</strong> {node.name} <span className="text-xs text-[#52606a]">{node.type}</span>
              </span>
              <button
                className={ghostButton}
                type="button"
                onClick={() => {
                  onNodesChange(nodes.filter((candidate) => candidate.nodeKey !== node.nodeKey));
                  onEdgesChange(edges.filter((edge) => edge.sourceNodeKey !== node.nodeKey && edge.targetNodeKey !== node.nodeKey));
                }}
              >
                Remove
              </button>
            </div>
          ))}
        </div>
      </form>
      <form
        className="grid gap-3 rounded-md border border-[#d6dee3] p-4"
        onSubmit={(event) => {
          void edgeForm.handleSubmit((value) => {
            onEdgesChange([...edges, value]);
            edgeForm.reset({ sourceNodeKey: '', targetNodeKey: '' });
          })(event);
        }}
      >
        <h3 className="font-semibold">Edges</h3>
        <select aria-label="Edge source node" className={inputClass} {...edgeForm.register('sourceNodeKey')}>
          <option value="">Source</option>
          {nodes.map((node) => (
            <option key={node.nodeKey} value={node.nodeKey}>
              {node.nodeKey}
            </option>
          ))}
        </select>
        <select aria-label="Edge target node" className={inputClass} {...edgeForm.register('targetNodeKey')}>
          <option value="">Target</option>
          {nodes.map((node) => (
            <option key={node.nodeKey} value={node.nodeKey}>
              {node.nodeKey}
            </option>
          ))}
        </select>
        <button className={secondaryButton} type="submit">
          Add edge
        </button>
        <div className="grid gap-2">
          {edges.map((edge, index) => (
            <div className="flex items-center justify-between rounded-md bg-[#f0f4f5] px-3 py-2 text-sm" key={`${edge.sourceNodeKey}-${edge.targetNodeKey}-${index}`}>
              <span>{edge.sourceNodeKey} -&gt; {edge.targetNodeKey}</span>
              <button className={ghostButton} type="button" onClick={() => onEdgesChange(edges.filter((_, edgeIndex) => edgeIndex !== index))}>
                Remove
              </button>
            </div>
          ))}
        </div>
      </form>
    </div>
  );
}

function RunPanel({ runId, edges }: { runId: string | null; edges: WorkflowEdge[] }) {
  const runQuery = useQuery({
    queryKey: ['workflow-run', runId],
    queryFn: () => getWorkflowRun(runId ?? ''),
    enabled: Boolean(runId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'SUCCEEDED' || status === 'FAILED' ? false : 1500;
    },
  });
  const tasksQuery = useQuery({
    queryKey: ['workflow-run-tasks', runId],
    queryFn: () => getWorkflowRunTasks(runId ?? ''),
    enabled: Boolean(runId),
    refetchInterval: runQuery.data?.status === 'SUCCEEDED' || runQuery.data?.status === 'FAILED' ? false : 1500,
  });

  if (!runId) {
    return (
      <section className="rounded-md border border-[#d6dee3] bg-white p-5">
        <h2 className="text-lg font-semibold">Run detail</h2>
        <p className="mt-2 text-sm text-[#52606a]">Start a published workflow to open run tracking.</p>
      </section>
    );
  }

  return (
    <section className="rounded-md border border-[#d6dee3] bg-white">
      <div className="border-b border-[#d6dee3] px-5 py-4">
        <h2 className="text-lg font-semibold">Run detail</h2>
        <p className="text-sm text-[#52606a]">
          {runQuery.data?.status ?? 'Loading'} - {runId}
        </p>
      </div>
      <div className="grid gap-5 p-5 lg:grid-cols-[1fr_320px]">
        <GraphView nodes={tasksToNodes(tasksQuery.data)} edges={edges} tasks={tasksQuery.data} />
        <RunSummary run={runQuery.data} tasks={tasksQuery.data ?? []} />
      </div>
    </section>
  );
}

function GraphView({
  nodes,
  edges,
  tasks,
}: {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  tasks?: WorkflowTask[];
}) {
  const positions = useMemo(
    () =>
      nodes.map((node, index) => ({
        node,
        x: 70 + index * 150,
        y: 90 + (index % 2) * 72,
      })),
    [nodes],
  );
  const width = Math.max(520, 140 + Math.max(positions.length - 1, 0) * 150);
  const taskByKey = new Map((tasks ?? []).map((task) => [task.nodeKey, task]));

  return (
    <div className="overflow-auto rounded-md border border-[#d6dee3] bg-[#fbfcfc]">
      <svg className="block h-[300px]" role="img" aria-label="Workflow graph" viewBox={`0 0 ${width} 300`}>
        <defs>
          <marker id="arrow" markerHeight="8" markerWidth="8" orient="auto" refX="7" refY="4">
            <path d="M0,0 L8,4 L0,8 Z" fill="#5e717b" />
          </marker>
        </defs>
        {edges.map((edge) => {
          const source = positions.find((position) => position.node.nodeKey === edge.sourceNodeKey);
          const target = positions.find((position) => position.node.nodeKey === edge.targetNodeKey);
          if (!source || !target) {
            return null;
          }
          return (
            <line
              key={`${edge.sourceNodeKey}-${edge.targetNodeKey}`}
              x1={source.x + 54}
              y1={source.y}
              x2={target.x - 54}
              y2={target.y}
              stroke="#5e717b"
              strokeWidth="2"
              markerEnd="url(#arrow)"
            />
          );
        })}
        {positions.map(({ node, x, y }) => {
          const task = taskByKey.get(node.nodeKey);
          return (
            <g key={node.nodeKey}>
              <rect x={x - 54} y={y - 28} width="108" height="56" rx="8" fill={statusFill(task?.status)} stroke="#9caeb8" />
              <text x={x} y={y - 4} textAnchor="middle" fontSize="14" fontWeight="700" fill="#17202a">
                {node.nodeKey}
              </text>
              <text x={x} y={y + 16} textAnchor="middle" fontSize="11" fill="#52606a">
                {task?.status ?? node.name}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

function RunSummary({ run, tasks }: { run?: WorkflowRun; tasks: WorkflowTask[] }) {
  return (
    <div className="rounded-md border border-[#d6dee3] p-4">
      <h3 className="font-semibold">Tasks</h3>
      <div className="mt-3 grid gap-2">
        {tasks.map((task) => (
          <div className="rounded-md bg-[#f0f4f5] px-3 py-2 text-sm" key={task.id}>
            <div className="flex items-center justify-between">
              <span>{task.nodeKey} <span className="text-xs text-[#52606a]">{task.type}</span></span>
              <span className="font-medium">{task.status}</span>
            </div>
            {task.result ? <p className="mt-1 break-words text-xs text-[#52606a]">{task.result}</p> : null}
          </div>
        ))}
      </div>
      {run?.failureMessage ? <p className="mt-3 text-sm text-[#a33a2a]">{run.failureMessage}</p> : null}
    </div>
  );
}

function ApprovalPanel({ runId }: { runId: string | null }) {
  const queryClient = useQueryClient();
  const approvalsQuery = useQuery({
    queryKey: ['approvals'],
    queryFn: listApprovals,
    refetchInterval: 3000,
  });
  const approveMutation = useMutation({
    mutationFn: approveTask,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['approvals'] });
      if (runId) {
        void queryClient.invalidateQueries({ queryKey: ['workflow-run', runId] });
        void queryClient.invalidateQueries({ queryKey: ['workflow-run-tasks', runId] });
      }
    },
  });
  const rejectMutation = useMutation({
    mutationFn: (task: ApprovalTask) => rejectTask(task.id, 'Rejected from workflow console'),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['approvals'] });
      if (runId) {
        void queryClient.invalidateQueries({ queryKey: ['workflow-run', runId] });
        void queryClient.invalidateQueries({ queryKey: ['workflow-run-tasks', runId] });
      }
    },
  });

  return (
    <section className="rounded-md border border-[#d6dee3] bg-white p-5">
      <h2 className="text-lg font-semibold">Approvals</h2>
      {approvalsQuery.data?.length ? (
        <div className="mt-3 grid gap-3">
          {approvalsQuery.data.map((task) => (
            <div className="rounded-md border border-[#d6dee3] p-3 text-sm" key={task.id}>
              <p className="font-medium">{task.nodeKey} - {task.description ?? 'Approval'}</p>
              <p className="mt-1 break-words text-[#52606a]">{task.prompt ?? 'Approval required'}</p>
              <div className="mt-3 flex gap-2">
                <button className={secondaryButton} type="button" onClick={() => approveMutation.mutate(task.id)}>
                  Approve
                </button>
                <button className={ghostButton} type="button" onClick={() => rejectMutation.mutate(task)}>
                  Reject
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="mt-2 text-sm text-[#52606a]">No approval tasks are waiting.</p>
      )}
      {[approvalsQuery.error, approveMutation.error, rejectMutation.error]
        .filter((error): error is Error => error instanceof Error)
        .map((error) => (
          <p className="mt-2 text-sm text-[#a33a2a]" key={error.message}>{error.message}</p>
        ))}
    </section>
  );
}

function ActionState({ errors }: { errors: string[] }) {
  if (!errors.length) {
    return null;
  }
  return (
    <div className="border-t border-[#d6dee3] bg-[#fff8f6] px-5 py-3 text-sm text-[#8f3528]">
      {errors.map((error) => (
        <p key={error}>{error}</p>
      ))}
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="grid gap-1 text-sm font-medium text-[#34434c]">
      {label}
      {children}
      {error ? <span className="text-[#a33a2a]">{error}</span> : null}
    </label>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-[#d6dee3] p-3">
      <p className="text-xs uppercase tracking-wide">{label}</p>
      <p className="mt-1 font-semibold text-[#17202a]">{value}</p>
    </div>
  );
}

function tasksToNodes(tasks: WorkflowTask[] | undefined): WorkflowNode[] {
  return (tasks ?? []).map((task) => ({
    nodeKey: task.nodeKey,
    type: task.type,
    name: task.status,
    configuration: '{}',
  }));
}

function modeButton(active: boolean) {
  return `rounded px-3 py-2 text-sm font-medium ${active ? 'bg-[#0a6d78] text-white' : 'text-[#52606a] hover:bg-[#eef3f4]'}`;
}

function statusFill(status?: string) {
  switch (status) {
    case 'SUCCEEDED':
      return '#dff3e8';
    case 'FAILED':
    case 'DEAD_LETTERED':
      return '#f8ddd8';
    case 'RUNNING':
    case 'DISPATCHED':
      return '#dcecff';
    case 'WAITING_APPROVAL':
      return '#fff1cc';
    case 'BLOCKED':
      return '#edf0f2';
    default:
      return '#ffffff';
  }
}

const inputClass =
  'w-full rounded-md border border-[#c8d4db] bg-white px-3 py-2 text-sm outline-none focus:border-[#0a6d78] focus:ring-2 focus:ring-[#b9dfe3]';
const primaryButton = 'rounded-md bg-[#0a6d78] px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60';
const secondaryButton =
  'rounded-md border border-[#b8c8d0] bg-white px-4 py-2 text-sm font-semibold text-[#24343d] hover:bg-[#eef3f4] disabled:cursor-not-allowed disabled:opacity-60';
const ghostButton = 'rounded px-2 py-1 text-sm font-medium text-[#52606a] hover:bg-[#eef3f4]';
