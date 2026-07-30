"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { CancellationStepper } from "@/components/cancellation-stepper";
import { getCurrentCancellationReason, saveCurrentCancellationReason, type CancellationReasonCode } from "@/lib/api/cancellation-reason";
import { HttpClientError } from "@/lib/http-client";

const options: ReadonlyArray<{ code: CancellationReasonCode; title: string; description: string }> = [
  { code: "THEFT", title: "Robo", description: "Tu dispositivo o medio de almacenamiento fue robado." },
  { code: "LOSS", title: "Pérdida", description: "Has perdido tu dispositivo o medio de almacenamiento." },
  { code: "DEVICE_OR_NUMBER_CHANGE", title: "Cambio de equipo o número", description: "Ya no usarás el equipo o número asociado." },
  { code: "SUSPECTED_UNAUTHORIZED_USE", title: "Sospecha de uso no autorizado", description: "Sospechas que alguien más pudo haber accedido." },
  { code: "OTHER", title: "Otro motivo", description: "Ninguna de las opciones anteriores describe mi caso." },
];

export function CancellationReasonTransition({ onBack, onContinue }: { onBack: () => void; onContinue: () => void }) {
  const [state, setState] = useState<"loading" | "ready" | "error">("loading");
  const [reason, setReason] = useState<CancellationReasonCode | null>(null);
  const [otherReason, setOtherReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false);

  const load = useCallback(async (signal?: AbortSignal) => {
    setState("loading");
    try {
      const response = await getCurrentCancellationReason();
      if (signal?.aborted) return;
      if (!response.data) throw new Error("Missing cancellation reason response");
      setReason(response.data.reasonCode ?? null);
      setOtherReason(response.data.otherReason ?? "");
      setState("ready");
    } catch (error) {
      if (signal?.aborted || (error instanceof HttpClientError && error.code === "REQUEST_ABORTED")) return;
      setState("error");
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const descriptionIsValid = reason !== "OTHER" || (otherReason.trim().length >= 10 && otherReason.trim().length <= 300);
  const canSubmit = reason !== null && descriptionIsValid && !submitting;

  const submit = async () => {
    if (!canSubmit || reason === null || inFlight.current) return;
    inFlight.current = true;
    setSubmitting(true);
    try {
      const response = await saveCurrentCancellationReason(reason, reason === "OTHER" ? otherReason.trim() : null);
      if (!response.data?.canContinue) throw new Error("Reason was not confirmed");
      onContinue();
    } catch {
      setState("error");
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  };

  return (
    <section className="mx-auto w-full max-w-[1040px]" aria-labelledby="reason-title">
      <div className="px-2 sm:px-8 lg:px-14">
        <CancellationStepper currentStep={3} navigableSteps={[2]} onNavigate={step => step === 2 && onBack()} />
      </div>
      <div className="mx-2 mt-6 rounded-[22px] border border-[#dfe7f3] bg-white/95 px-4 py-7 sm:mx-8 sm:px-8 sm:py-9 lg:mx-14">
        {state === "loading" ? <Loading /> : null}
        {state === "error" ? <ErrorState onRetry={() => void load()} /> : null}
        {state === "ready" ? <div className="mx-auto max-w-[720px]">
          <header className="text-center">
            <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 3 DE 5</p>
            <h1 id="reason-title" className="mt-4 text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">¿Cuál es el motivo de la cancelación?</h1>
            <p className="mt-3 text-sm leading-6 text-[#52678f] sm:text-base">Selecciona la causa que mejor describa tu situación.</p>
          </header>
          <fieldset className="mt-7 space-y-3">
            <legend className="sr-only">Motivo de cancelación</legend>
            {options.map(option => {
              const selected = reason === option.code;
              return <label key={option.code} className={`block cursor-pointer rounded-xl border px-4 py-4 transition-[border-color,background-color] duration-200 ${selected ? "border-[#1768f2] bg-[#f5f8ff]" : "border-[#dbe4f1] bg-white hover:border-[#8eafe9] hover:bg-[#fbfdff]"}`}>
                <span className="flex items-start gap-4">
                  <input type="radio" name="cancellation-reason" checked={selected} onChange={() => setReason(option.code)} className="mt-1 size-5 cursor-pointer accent-[#1768f2] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df]" />
                  <span><strong className="block text-[#061a50]">{option.title}</strong><span className="mt-1 block text-sm leading-5 text-[#52678f]">{option.description}</span></span>
                </span>
                {option.code === "OTHER" && selected ? <span className="mt-4 block border-t border-[#e3e9f2] pt-4 pl-9">
                  <span className="mb-2 block text-sm font-bold text-[#172b59]">Describe brevemente el motivo</span>
                  <textarea value={otherReason} onChange={event => setOtherReason(event.target.value)} minLength={10} maxLength={300} rows={4}
                    className="w-full resize-y rounded-lg border border-[#aebdd3] bg-white px-4 py-3 text-[#172b59] outline-none transition-colors focus:border-[#0755df] focus:ring-2 focus:ring-[#dce9ff]" aria-describedby="other-reason-help" />
                  <span id="other-reason-help" className="mt-1 flex justify-between gap-3 text-xs text-[#65779b]"><span>Mínimo 10 caracteres.</span><span>{otherReason.length}/300</span></span>
                </span> : null}
              </label>;
            })}
          </fieldset>
          <div className="mt-6 flex flex-col-reverse justify-between gap-3 border-t border-[#e1e8f2] pt-5 sm:flex-row">
            <button type="button" onClick={onBack} className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-lg px-6 font-bold text-[#173a78] hover:bg-[#f1f5fb] sm:w-[280px]"><BackIcon /> Regresar</button>
            <button type="button" onClick={() => void submit()} disabled={!canSubmit} className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-3 rounded-lg bg-reniec-red px-6 font-bold text-white hover:bg-[#a8003f] disabled:cursor-default disabled:bg-[#c9cfdb] sm:w-[280px]">{submitting ? "Guardando motivo…" : "Continuar"}<ArrowIcon /></button>
          </div>
        </div> : null}
      </div>
    </section>
  );
}

function Loading() { return <div className="grid min-h-[420px] place-items-center text-center" aria-live="polite"><div><span className="mx-auto block size-10 animate-spin rounded-full border-4 border-[#d6e2f7] border-t-reniec-red" /><p className="mt-4 font-semibold text-[#52678f]">Cargando el motivo…</p></div></div>; }
function ErrorState({ onRetry }: { onRetry: () => void }) { return <div className="grid min-h-[390px] place-items-center text-center" role="alert"><div><h1 id="reason-title" className="text-2xl font-black text-[#061a50]">No pudimos cargar el motivo</h1><p className="mt-3 text-[#52678f]">Inténtalo nuevamente.</p><button type="button" onClick={onRetry} className="mt-6 min-h-11 cursor-pointer rounded-lg bg-reniec-red px-6 font-bold text-white">Reintentar</button></div></div>; }
function BackIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M19 12H5m5-5-5 5 5 5" /></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5" /></svg>; }
