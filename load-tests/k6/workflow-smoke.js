import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.TASKFORGE_K6_VUS || 3),
  duration: __ENV.TASKFORGE_K6_DURATION || '20s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
  },
};

const baseUrl = __ENV.TASKFORGE_BASE_URL || 'http://localhost:8080';

function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

export default function () {
  const id = `${__VU}-${__ITER}-${Date.now()}`;
  const auth = http.post(`${baseUrl}/api/auth/register`, JSON.stringify({
    email: `k6-${id}@example.com`,
    password: 'correct horse battery',
    organizationName: `k6 org ${id}`,
  }), jsonHeaders());
  check(auth, { 'registered': (response) => response.status === 201 });
  if (auth.status !== 201) {
    return;
  }

  const token = auth.json('accessToken');
  const workflow = http.post(`${baseUrl}/api/workflows`, JSON.stringify({
    name: `k6 workflow ${id}`,
    description: 'k6 smoke workflow',
  }), jsonHeaders(token));
  check(workflow, { 'workflow created': (response) => response.status === 201 });
  if (workflow.status !== 201) {
    return;
  }

  const workflowId = workflow.json('id');
  const draft = http.patch(`${baseUrl}/api/workflows/${workflowId}/draft`, JSON.stringify({
    nodes: [
      { nodeKey: 'A', type: 'NO_OP', name: 'start', configuration: '{}' },
      { nodeKey: 'B', type: 'TRANSFORM', name: 'transform', configuration: '{"value":"k6"}' },
    ],
    edges: [{ sourceNodeKey: 'A', targetNodeKey: 'B' }],
  }), jsonHeaders(token));
  check(draft, { 'draft saved': (response) => response.status === 200 });

  const publish = http.post(`${baseUrl}/api/workflows/${workflowId}/publish`, null, jsonHeaders(token));
  check(publish, { 'workflow published': (response) => response.status === 200 });

  const run = http.post(`${baseUrl}/api/workflows/${workflowId}/runs`, null, jsonHeaders(token));
  check(run, { 'run started': (response) => response.status === 201 });
  sleep(1);
}
