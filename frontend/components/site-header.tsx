export function SiteHeader() {
  return (
    <header className="border-b border-slate-800 bg-slate-950 text-white">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-start justify-between gap-3 px-4 py-4 sm:flex-row sm:items-center sm:px-6 lg:px-8">
        <div className="min-w-0">
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-300">
            RENIEC
          </p>
          <p className="mt-1 text-sm font-semibold leading-snug text-white sm:text-base">
            Sistema de Gestión de Certificados Digitales
          </p>
        </div>

        <p className="rounded-full border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs font-semibold text-slate-200">
          Entorno técnico
        </p>
      </div>
    </header>
  );
}
