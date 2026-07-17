"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { getSystemStatus } from "@/lib/api/system-status";
import { HttpClientError } from "@/lib/http-client";

export type IntegrationState =
  | { kind: "checking" }
  | { kind: "available"; correlationId?: string }
  | { kind: "unavailable"; correlationId?: string };

export function IntegrationStatusIndicator() {
  const [state, setState] = useState<IntegrationState>({ kind: "checking" });
  const mounted = useRef(true);
  const requestController = useRef<AbortController | null>(null);

  const check = useCallback(async () => {
    requestController.current?.abort();
    const controller = new AbortController();
    requestController.current = controller;
    setState({ kind: "checking" });

    try {
      const result = await getSystemStatus(controller.signal);
      if (mounted.current) setState({ kind: "available", correlationId: result.correlationId });
    } catch (error) {
      if (mounted.current && !(error instanceof HttpClientError && error.code === "REQUEST_ABORTED")) {
        setState({
          kind: "unavailable",
          correlationId: error instanceof HttpClientError ? error.correlationId : undefined,
        });
      }
    }
  }, []);

  useEffect(() => {
    mounted.current = true;
    void check();
    return () => {
      mounted.current = false;
      requestController.current?.abort();
    };
  }, [check]);

  return <IntegrationStatusView state={state} onRetry={() => void check()} />;
}

export function IntegrationStatusView({ state, onRetry }: { state: IntegrationState; onRetry?: () => void }) {
  return (
    <div className="mt-7" role="status" aria-live="polite" aria-atomic="true">
      {state.kind === "checking" && (
        <p className="flex items-center gap-2 font-semibold text-slate-700">
          <span className="size-2.5 animate-pulse rounded-full bg-amber-500" aria-hidden="true" />
          Comprobando integración…
        </p>
      )}

      {state.kind === "available" && (
        <div>
          <p className="flex items-center gap-2 font-bold text-emerald-800">
            <span className="size-2.5 rounded-full bg-emerald-600" aria-hidden="true" />
            Integración disponible
          </p>
          <p className="mt-2 leading-6 text-slate-700">Backend y base de datos MySQL responden correctamente.</p>
        </div>
      )}

      {state.kind === "unavailable" && (
        <div>
          <p className="flex items-center gap-2 font-bold text-rose-800">
            <span className="size-2.5 rounded-full bg-rose-700" aria-hidden="true" />
            Integración no disponible
          </p>
          <p className="mt-2 leading-6 text-slate-700">
            No fue posible comprobar el backend y MySQL. La página puede seguir utilizándose.
          </p>
          {state.correlationId && (
            <p className="mt-2 break-all text-xs text-slate-500">Referencia: {state.correlationId}</p>
          )}
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="mt-4 rounded-lg border border-slate-300 bg-white px-4 py-2 font-semibold text-slate-800 transition hover:border-slate-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-700"
            >
              Reintentar comprobación
            </button>
          )}
        </div>
      )}
    </div>
  );
}
