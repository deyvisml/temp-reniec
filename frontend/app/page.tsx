export default function HomePage() {
  const environment = process.env.NEXT_PUBLIC_APP_ENV ?? "local";

  return (
    <section
      aria-labelledby="preparation-title"
      className="grid overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_24px_70px_-40px_rgba(15,23,42,0.45)] md:grid-cols-[minmax(0,1fr)_18rem]"
    >
      <div className="p-6 sm:p-9 lg:p-12">
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-rose-800">
          Base de aplicación
        </p>
        <h1
          id="preparation-title"
          className="mt-4 max-w-3xl text-[clamp(2rem,5vw,3.75rem)] font-bold leading-[1.05] tracking-[-0.035em] text-slate-950"
        >
          Cancelación de certificados digitales
        </h1>
        <p className="mt-5 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg sm:leading-8">
          Proyecto en preparación. Esta página confirma que la base técnica del frontend está
          disponible para continuar el desarrollo.
        </p>
      </div>

      <div className="border-t border-slate-200 bg-slate-50 p-6 md:border-l md:border-t-0 md:p-8">
        <p className="flex items-center gap-2 text-sm font-bold text-emerald-800">
          <span className="size-2.5 rounded-full bg-emerald-600" aria-hidden="true" />
          Base técnica operativa
        </p>

        <dl className="mt-7 grid gap-5 text-sm">
          <div>
            <dt className="font-semibold text-slate-500">Aplicación</dt>
            <dd className="mt-1 font-bold text-slate-900">Frontend temporal</dd>
          </div>
          <div>
            <dt className="font-semibold text-slate-500">Ambiente</dt>
            <dd className="mt-1 font-bold text-slate-900">{environment}</dd>
          </div>
          <div>
            <dt className="font-semibold text-slate-500">Integración</dt>
            <dd className="mt-1 leading-6 text-slate-700">Preparada para una tarea posterior</dd>
          </div>
        </dl>
      </div>
    </section>
  );
}
