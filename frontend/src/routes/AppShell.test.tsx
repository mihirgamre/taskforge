import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { RouterProvider } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { router } from './router';

describe('AppShell', () => {
  it('renders the foundation overview', () => {
    const queryClient = new QueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    );

    expect(screen.getByRole('heading', { name: /workflow orchestration foundation/i })).toBeInTheDocument();
    expect(screen.getByText(/product workflow features are intentionally not implemented/i)).toBeInTheDocument();
  });
});

