"use client";

export default function ErrorPage({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <section aria-labelledby="error-title" className="max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-9">
      <p className="text-xs font-bold uppercase tracking-[0.18em] text-rose-800">
        Error inesperado
      </p>
      <h1 id="error-title" className="mt-3 text-3xl font-bold tracking-tight text-slate-950">
        No pudimos mostrar esta sección
      </h1>
      <p className="mt-4 leading-7 text-slate-600">
        Intenta nuevamente. Si el problema continúa, vuelve a la página de inicio.
      </p>
      <button
        type="button"
        onClick={reset}
        className="mt-6 min-h-11 rounded-lg bg-slate-950 px-5 py-2.5 text-sm font-bold text-white transition-colors hover:bg-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#f4b400]"
      >
        Intentar nuevamente
      </button>
    </section>
  );
}
