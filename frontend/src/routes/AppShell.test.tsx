import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { RouterProvider } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createAppRouter } from './router';

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const router = createAppRouter();

  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

describe('AppShell', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.localStorage.clear();
  });

  it('renders the workflow console entry screen', () => {
    renderApp();

    expect(screen.getByRole('heading', { name: /workflow operations console/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /build and run authenticated workflow dags/i })).toBeInTheDocument();
  });

  it('renders authenticated workflow management data', async () => {
    window.localStorage.setItem('taskforge.accessToken', 'access-token');
    window.localStorage.setItem('taskforge.refreshToken', 'refresh-token');
    vi.spyOn(window, 'fetch').mockImplementation((input) => {
      const path = input instanceof Request ? input.url : String(input);
      if (path.endsWith('/api/auth/refresh')) {
        return Promise.resolve(jsonResponse({
          accessToken: 'new-access-token',
          refreshToken: 'new-refresh-token',
          user: { id: '11111111-1111-4111-8111-111111111111', email: 'user@example.com' },
          organization: { id: '22222222-2222-4222-8222-222222222222', name: 'TaskForge Lab', role: 'OWNER' },
        }));
      }
      if (path.endsWith('/api/workflows')) {
        return Promise.resolve(jsonResponse([
          {
            id: '33333333-3333-4333-8333-333333333333',
            name: 'Build pipeline',
            description: 'Demo',
            status: 'ACTIVE',
            draftVersionId: '44444444-4444-4444-8444-444444444444',
            draftVersionNumber: 2,
            createdAt: '2026-08-30T00:00:00Z',
            updatedAt: '2026-08-30T00:00:00Z',
          },
        ]));
      }
      if (path.endsWith('/api/workflows/33333333-3333-4333-8333-333333333333/draft')) {
        return Promise.resolve(jsonResponse({
          workflowId: '33333333-3333-4333-8333-333333333333',
          versionId: '44444444-4444-4444-8444-444444444444',
          versionNumber: 2,
          status: 'DRAFT',
          nodes: [{ nodeKey: 'A', type: 'NO_OP', name: 'Intake', configuration: '{}' }],
          edges: [],
        }));
      }
      return Promise.resolve(jsonResponse({}, 404));
    });

    renderApp();

    await waitFor(() => expect(screen.getByText('Build pipeline')).toBeInTheDocument());
    expect(screen.getByText('TaskForge Lab')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /workflow builder/i })).toBeInTheDocument();
  });
});

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
