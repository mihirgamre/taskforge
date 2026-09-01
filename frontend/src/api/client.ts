import { z } from 'zod';
import { getStoredSession, storeSession, type StoredSession } from './session';

const foundationSchema = z.object({
  service: z.string(),
  version: z.string(),
});

export type FoundationServiceInfo = z.infer<typeof foundationSchema>;

export async function getFoundationInfo(): Promise<FoundationServiceInfo> {
  const response = await fetch('/api/foundation');

  if (!response.ok) {
    throw new Error(`Foundation request failed with status ${response.status}`);
  }

  return foundationSchema.parse(await response.json());
}

const authResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  user: z.object({
    id: z.string().uuid(),
    email: z.string().email(),
  }),
  organization: z.object({
    id: z.string().uuid(),
    name: z.string(),
    role: z.string(),
  }),
});

const workflowSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  description: z.string().nullable(),
  status: z.string(),
  draftVersionId: z.string().uuid().nullable(),
  draftVersionNumber: z.number().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

const workflowNodeSchema = z.object({
  nodeKey: z.string(),
  type: z.enum(['NO_OP', 'HTTP', 'TRANSFORM', 'APPROVAL', 'NOTIFICATION']),
  name: z.string(),
  configuration: z.string().nullable(),
});

const workflowEdgeSchema = z.object({
  sourceNodeKey: z.string(),
  targetNodeKey: z.string(),
});

const draftSchema = z.object({
  workflowId: z.string().uuid(),
  versionId: z.string().uuid(),
  versionNumber: z.number(),
  status: z.string(),
  nodes: z.array(workflowNodeSchema),
  edges: z.array(workflowEdgeSchema),
});

const validationSchema = z.object({
  valid: z.boolean(),
  errors: z.array(z.string()),
});

const runSchema = z.object({
  id: z.string().uuid(),
  workflowId: z.string().uuid(),
  workflowVersionId: z.string().uuid(),
  status: z.string(),
  startedAt: z.string().nullable(),
  completedAt: z.string().nullable(),
  failureMessage: z.string().nullable(),
});

const taskSchema = z.object({
  id: z.string().uuid(),
  nodeKey: z.string(),
  type: z.enum(['NO_OP', 'HTTP', 'TRANSFORM', 'APPROVAL', 'NOTIFICATION']),
  status: z.string(),
  attemptCount: z.number(),
  result: z.string().nullable(),
});

const approvalSchema = z.object({
  id: z.string().uuid(),
  workflowRunId: z.string().uuid(),
  nodeKey: z.string(),
  description: z.string().nullable(),
  status: z.string(),
  prompt: z.string().nullable(),
});

export type AuthResponse = z.infer<typeof authResponseSchema>;
export type WorkflowSummary = z.infer<typeof workflowSchema>;
export type WorkflowNode = z.infer<typeof workflowNodeSchema>;
export type WorkflowEdge = z.infer<typeof workflowEdgeSchema>;
export type WorkflowDraft = z.infer<typeof draftSchema>;
export type WorkflowValidation = z.infer<typeof validationSchema>;
export type WorkflowRun = z.infer<typeof runSchema>;
export type WorkflowTask = z.infer<typeof taskSchema>;
export type ApprovalTask = z.infer<typeof approvalSchema>;

type RequestOptions = {
  method?: string;
  body?: unknown;
  authenticated?: boolean;
};

async function apiRequest<T>(
  path: string,
  schema: z.ZodType<T>,
  { method = 'GET', body, authenticated = true }: RequestOptions = {},
): Promise<T> {
  const session = getStoredSession();
  const headers = new Headers();
  headers.set('Accept', 'application/json');
  if (body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (authenticated && session) {
    headers.set('Authorization', `Bearer ${session.accessToken}`);
  }

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const errorBody = z.object({ message: z.string().optional() }).parse(await response.json());
      message = errorBody.message ?? message;
    } catch {
      // Keep the generic status message when the server returns an empty or unexpected body.
    }
    throw new Error(message);
  }

  return schema.parse(await response.json());
}

export async function register(input: {
  email: string;
  password: string;
  organizationName: string;
}): Promise<AuthResponse> {
  const response = await apiRequest('/api/auth/register', authResponseSchema, {
    method: 'POST',
    body: input,
    authenticated: false,
  });
  storeSession(response);
  return response;
}

export async function login(input: { email: string; password: string }): Promise<AuthResponse> {
  const response = await apiRequest('/api/auth/login', authResponseSchema, {
    method: 'POST',
    body: input,
    authenticated: false,
  });
  storeSession(response);
  return response;
}

export async function restoreSession(): Promise<AuthResponse | null> {
  const session = getStoredSession();
  if (!session) {
    return null;
  }
  try {
    const response = await apiRequest('/api/auth/refresh', authResponseSchema, {
      method: 'POST',
      body: { refreshToken: session.refreshToken },
      authenticated: false,
    });
    storeSession(response);
    return response;
  } catch {
    return null;
  }
}

export function currentSession(): StoredSession | null {
  return getStoredSession();
}

export function listWorkflows(): Promise<WorkflowSummary[]> {
  return apiRequest('/api/workflows', z.array(workflowSchema));
}

export function createWorkflow(input: { name: string; description: string }): Promise<WorkflowSummary> {
  return apiRequest('/api/workflows', workflowSchema, { method: 'POST', body: input });
}

export function getWorkflowDraft(workflowId: string): Promise<WorkflowDraft> {
  return apiRequest(`/api/workflows/${workflowId}/draft`, draftSchema);
}

export function saveWorkflowDraft(workflowId: string, draft: {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
}): Promise<WorkflowDraft> {
  return apiRequest(`/api/workflows/${workflowId}/draft`, draftSchema, {
    method: 'PATCH',
    body: draft,
  });
}

export function validateWorkflow(workflowId: string): Promise<WorkflowValidation> {
  return apiRequest(`/api/workflows/${workflowId}/validate`, validationSchema, { method: 'POST' });
}

export function publishWorkflow(workflowId: string): Promise<WorkflowDraft> {
  return apiRequest(`/api/workflows/${workflowId}/publish`, draftSchema, { method: 'POST' });
}

export function startWorkflowRun(workflowId: string): Promise<WorkflowRun> {
  return apiRequest(`/api/workflows/${workflowId}/runs`, runSchema, { method: 'POST' });
}

export function getWorkflowRun(runId: string): Promise<WorkflowRun> {
  return apiRequest(`/api/workflow-runs/${runId}`, runSchema);
}

export function getWorkflowRunTasks(runId: string): Promise<WorkflowTask[]> {
  return apiRequest(`/api/workflow-runs/${runId}/tasks`, z.array(taskSchema));
}

export function listApprovals(): Promise<ApprovalTask[]> {
  return apiRequest('/api/approvals', z.array(approvalSchema));
}

export function approveTask(taskId: string): Promise<ApprovalTask> {
  return apiRequest(`/api/approvals/${taskId}/approve`, approvalSchema, { method: 'POST' });
}

export function rejectTask(taskId: string, reason: string): Promise<ApprovalTask> {
  return apiRequest(`/api/approvals/${taskId}/reject`, approvalSchema, {
    method: 'POST',
    body: { reason },
  });
}
