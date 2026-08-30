import { Outlet, NavLink } from 'react-router-dom';
import { ErrorBoundary } from '../components/ErrorBoundary';

export function AppShell() {
  return (
    <ErrorBoundary>
      <div className="min-h-screen bg-[#f5f7f8] text-[#17202a]">
        <header className="border-b border-[#d6dee3] bg-white">
          <div className="mx-auto flex max-w-7xl flex-col gap-4 px-6 py-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-medium uppercase tracking-wide text-[#4e6b74]">TaskForge</p>
              <h1 className="text-xl font-semibold">Workflow operations console</h1>
            </div>
            <nav aria-label="Primary" className="flex gap-4 text-sm font-medium">
              <NavLink to="/" className={({ isActive }) => (isActive ? 'text-[#0a6d78]' : 'text-[#52606a]')}>
                Workflows
              </NavLink>
              <NavLink to="/status" className={({ isActive }) => (isActive ? 'text-[#0a6d78]' : 'text-[#52606a]')}>
                Status
              </NavLink>
            </nav>
          </div>
        </header>
        <Outlet />
      </div>
    </ErrorBoundary>
  );
}
