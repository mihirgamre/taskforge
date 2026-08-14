export function StatusPage() {
  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <h2 className="text-2xl font-semibold">Implementation status</h2>
      <dl className="mt-6 grid gap-4 md:grid-cols-3">
        <div className="rounded-md border border-[#d5ddd3] bg-white p-4">
          <dt className="text-sm font-medium text-[#52615a]">Backend</dt>
          <dd className="mt-1 text-lg font-semibold">Foundation only</dd>
        </div>
        <div className="rounded-md border border-[#d5ddd3] bg-white p-4">
          <dt className="text-sm font-medium text-[#52615a]">Frontend</dt>
          <dd className="mt-1 text-lg font-semibold">Shell only</dd>
        </div>
        <div className="rounded-md border border-[#d5ddd3] bg-white p-4">
          <dt className="text-sm font-medium text-[#52615a]">Workflows</dt>
          <dd className="mt-1 text-lg font-semibold">Not started</dd>
        </div>
      </dl>
    </main>
  );
}

