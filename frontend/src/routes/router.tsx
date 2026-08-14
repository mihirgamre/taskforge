import { createBrowserRouter } from 'react-router-dom';
import { AppShell } from './AppShell';
import { OverviewPage } from './OverviewPage';
import { StatusPage } from './StatusPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <OverviewPage /> },
      { path: 'status', element: <StatusPage /> },
    ],
  },
]);

