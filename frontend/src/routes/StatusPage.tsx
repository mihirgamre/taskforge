export function StatusPage() {
  return (
    <main className="mx-auto max-w-7xl px-6 py-8">
      <h2 className="text-2xl font-semibold">Implementation status</h2>
      <dl className="mt-6 grid gap-4 md:grid-cols-3">
        <div className="rounded-md border border-[#d6dee3] bg-white p-4">
          <dt className="text-sm font-medium text-[#52606a]">Backend</dt>
          <dd className="mt-1 text-lg font-semibold">M3 complete</dd>
        </div>
        <div className="rounded-md border border-[#d6dee3] bg-white p-4">
          <dt className="text-sm font-medium text-[#52606a]">Frontend</dt>
          <dd className="mt-1 text-lg font-semibold">M4 console</dd>
        </div>
        <div className="rounded-md border border-[#d6dee3] bg-white p-4">
          <dt className="text-sm font-medium text-[#52606a]">Execution</dt>
          <dd className="mt-1 text-lg font-semibold">DAG + reliability active</dd>
        </div>
      </dl>
    </main>
  );
}
