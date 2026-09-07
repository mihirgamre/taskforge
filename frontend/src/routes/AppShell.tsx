import { Outlet, NavLink } from 'react-router-dom';
import { ErrorBoundary } from '../components/ErrorBoundary';

export function AppShell() {
  return (
    <ErrorBoundary>
      <div className="min-h-screen bg-[linear-gradient(180deg,#eef4f2_0%,#f7f8f3_42%,#f4f0e8_100%)] text-[#17202a]">
        <header className="sticky top-0 z-20 border-b border-white/70 bg-white/82 shadow-sm shadow-[#17202a]/5 backdrop-blur">
          <div className="mx-auto flex max-w-7xl flex-col gap-4 px-6 py-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-wide text-[#0a6d78]">TaskForge</p>
              <h1 className="text-xl font-semibold text-[#17202a]">Workflow operations console</h1>
            </div>
            <nav aria-label="Primary" className="inline-flex w-fit rounded-md border border-[#d6dee3] bg-[#f8faf8] p-1 text-sm font-semibold shadow-inner shadow-white">
              <NavLink
                to="/"
                className={({ isActive }) =>
                  `rounded px-4 py-2 transition duration-200 ${
                    isActive ? 'bg-[#0a6d78] text-white shadow-sm' : 'text-[#52606a] hover:bg-white hover:text-[#17202a]'
                  }`
                }
              >
                Workflows
              </NavLink>
              <NavLink
                to="/status"
                className={({ isActive }) =>
                  `rounded px-4 py-2 transition duration-200 ${
                    isActive ? 'bg-[#0a6d78] text-white shadow-sm' : 'text-[#52606a] hover:bg-white hover:text-[#17202a]'
                  }`
                }
              >
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
