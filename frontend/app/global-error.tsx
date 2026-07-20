"use client";

export default function GlobalError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="es">
      <body className="min-h-dvh bg-slate-100 p-6 text-slate-950">
        <main className="mx-auto mt-16 max-w-2xl rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
          <h1 className="text-3xl font-bold tracking-tight">La aplicación no está disponible</h1>
          <p className="mt-4 leading-7 text-slate-600">
            Ocurrió un error inesperado. Puedes intentar cargar la aplicación nuevamente.
          </p>
          <button
            type="button"
            onClick={reset}
            className="mt-6 min-h-11 rounded-lg bg-slate-950 px-5 py-2.5 text-sm font-bold text-white transition-colors hover:bg-slate-800 focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400]"
          >
            Reintentar
          </button>
        </main>
      </body>
    </html>
  );
}
