export default function NotFound() {
  return (
    <section aria-labelledby="not-found-title" className="max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-9">
      <p className="text-xs font-bold uppercase tracking-[0.18em] text-rose-800">Error 404</p>
      <h1 id="not-found-title" className="mt-3 text-3xl font-bold tracking-tight text-slate-950">
        Recurso no encontrado
      </h1>
      <p className="mt-4 leading-7 text-slate-600">
        La dirección solicitada no está disponible. Puedes volver a la página temporal de inicio.
      </p>
      <a
        href="/"
        className="mt-6 inline-flex min-h-11 items-center rounded-lg bg-slate-950 px-5 py-2.5 text-sm font-bold text-white hover:bg-slate-800"
      >
        Volver al inicio
      </a>
    </section>
  );
}
