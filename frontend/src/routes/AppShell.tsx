import { Outlet, NavLink } from 'react-router-dom';
import { ErrorBoundary } from '../components/ErrorBoundary';

export function AppShell() {
  return (
    <ErrorBoundary>
      <div className="min-h-screen bg-[#f7f8f3] text-[#17202a]">
        <header className="border-b border-[#d5ddd3] bg-white">
          <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
            <div>
              <p className="text-sm font-medium uppercase tracking-wide text-[#56715d]">TaskForge</p>
              <h1 className="text-xl font-semibold">Workflow orchestration foundation</h1>
            </div>
            <nav aria-label="Primary" className="flex gap-4 text-sm font-medium">
              <NavLink to="/" className={({ isActive }) => (isActive ? 'text-[#0f6b54]' : 'text-[#52615a]')}>
                Overview
              </NavLink>
              <NavLink to="/status" className={({ isActive }) => (isActive ? 'text-[#0f6b54]' : 'text-[#52615a]')}>
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

