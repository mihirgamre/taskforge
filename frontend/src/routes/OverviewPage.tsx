export function OverviewPage() {
  return (
    <main className="mx-auto grid max-w-6xl gap-6 px-6 py-8 md:grid-cols-[2fr_1fr]">
      <section>
        <h2 className="text-2xl font-semibold">Phase 0 foundation</h2>
        <p className="mt-3 max-w-2xl text-[#52615a]">
          This shell is ready for the first implementation slice. Product workflow features are intentionally not implemented yet.
        </p>
      </section>
      <aside className="rounded-md border border-[#d5ddd3] bg-white p-5">
        <h3 className="font-semibold">Service boundaries</h3>
        <ul className="mt-3 space-y-2 text-sm text-[#52615a]">
          <li>Control plane API</li>
          <li>Scheduler</li>
          <li>Worker</li>
        </ul>
      </aside>
    </main>
  );
}

