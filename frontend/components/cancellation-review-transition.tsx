"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { CancellationStepper } from "@/components/cancellation-stepper";
import {
  confirmCurrentCancellation,
  getCurrentCancellationReview,
  type CancellationReview,
} from "@/lib/api/cancellation-confirmation";
import { HttpClientError } from "@/lib/http-client";

type ReviewState =
  | { kind: "loading" }
  | { kind: "ready"; data: CancellationReview }
  | { kind: "error"; title: string; description: string; reload: boolean };

const dateFormatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "medium",
  timeStyle: "short",
  hour12: false,
});

export function CancellationReviewTransition({ onBack }: { onBack: () => void }) {
  const [state, setState] = useState<ReviewState>({ kind: "loading" });
  const [accepted, setAccepted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const submissionInFlight = useRef(false);

  const load = useCallback(async (signal?: AbortSignal) => {
    setState({ kind: "loading" });
    setAccepted(false);
    try {
      const result = await getCurrentCancellationReview(signal);
      if (!result.data) throw new Error("Missing review response");
      setState({ kind: "ready", data: result.data });
    } catch (error) {
      if (error instanceof HttpClientError && error.code === "REQUEST_ABORTED") return;
      if (error instanceof HttpClientError && (error.status === 401 || error.code.startsWith("SESSION_"))) {
        window.location.assign("/");
        return;
      }
      setState(errorState(error));
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const submit = async () => {
    if (state.kind !== "ready" || state.data.confirmed || !accepted || submissionInFlight.current) return;
    submissionInFlight.current = true;
    setSubmitting(true);
    try {
      const result = await confirmCurrentCancellation(state.data.consentVersion);
      if (!result.data?.confirmed) throw new Error("Confirmation was not persisted");
      setState({ kind: "ready", data: result.data });
    } catch (error) {
      if (error instanceof HttpClientError && (error.status === 401 || error.code.startsWith("SESSION_"))) {
        window.location.assign("/");
        return;
      }
      setState(errorState(error));
    } finally {
      submissionInFlight.current = false;
      setSubmitting(false);
    }
  };

  return (
    <section className="mx-auto w-full max-w-[1040px]" aria-labelledby="review-title">
      <div className="px-2 sm:px-8 lg:px-14">
        <CancellationStepper
          currentStep={4}
          navigableSteps={state.kind === "ready" && !state.data.confirmed && !submitting ? [3] : []}
          onNavigate={step => { if (step === 3) onBack(); }}
        />
      </div>
      <div className="mx-2 mt-6 rounded-[22px] border border-[#dfe7f3] bg-white/95 px-4 py-7 sm:mx-8 sm:px-8 sm:py-9 lg:mx-14">
        {state.kind === "loading" ? <LoadingState /> : null}
        {state.kind === "error" ? <ErrorState state={state} onRetry={() => void load()} /> : null}
        {state.kind === "ready" ? (
          <ReviewView
            review={state.data}
            accepted={accepted}
            submitting={submitting}
            onAccepted={setAccepted}
            onBack={onBack}
            onConfirm={() => void submit()}
          />
        ) : null}
      </div>
    </section>
  );
}

export function ReviewView({ review, accepted, submitting, onAccepted, onBack, onConfirm }: {
  review: CancellationReview;
  accepted: boolean;
  submitting: boolean;
  onAccepted: (value: boolean) => void;
  onBack: () => void;
  onConfirm: () => void;
}) {
  if (review.confirmed) return <ConfirmedState review={review} />;

  return (
    <div className="mx-auto max-w-[820px]">
      <header className="text-center">
        <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 4 DE 5</p>
        <h1 id="review-title" className="mt-4 text-balance text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">
          Revisa y confirma la cancelación
        </h1>
        <p className="mx-auto mt-3 max-w-[640px] text-pretty text-sm leading-6 text-[#52678f] sm:text-base">
          Verifica la información antes de continuar. Después de confirmar ya no podrás cambiarla.
        </p>
      </header>

      <dl className="mt-7 overflow-hidden rounded-xl border border-[#dce5f2] bg-[#fbfdff]">
        <SummaryRow label="DNI" value={review.maskedDni} />
        <SummaryRow label="Motivo" value={review.reasonLabel} />
        {review.otherReason ? <SummaryRow label="Descripción" value={review.otherReason} /> : null}
      </dl>

      <section className="mt-6" aria-labelledby="selected-certificates-title">
        <h2 id="selected-certificates-title" className="text-base font-black text-[#0a2259]">
          Certificados seleccionados <span className="ml-1 text-reniec-red">({review.certificates.length})</span>
        </h2>
        <div className="mt-3 divide-y divide-[#dce5f2] rounded-xl border border-[#dce5f2]">
          {review.certificates.map(certificate => (
            <article key={`${certificate.orderNumber}-${certificate.maskedUuid}`} className="grid gap-2 px-4 py-4 sm:grid-cols-3 sm:items-center">
              <Data label="N.º de orden" value={certificate.orderNumber} strong />
              <Data label="Fecha de creación" value={formatDate(certificate.emissionCreatedAt)} />
              <Data label="Identificador" value={certificate.maskedUuid} mono />
            </article>
          ))}
        </div>
      </section>

      <section className="mt-6 rounded-xl border border-[#f1c66d] bg-[#fffaf0] p-4" aria-labelledby="consequences-title">
        <h2 id="consequences-title" className="font-black text-[#6b4300]">Antes de confirmar</h2>
        <ul className="mt-2 space-y-2 text-sm leading-6 text-[#6b4c1c]">
          {review.consequences.map(item => <li key={item} className="flex gap-2"><span aria-hidden="true">•</span><span>{item}</span></li>)}
        </ul>
      </section>

      <label className="mt-5 flex cursor-pointer items-start gap-3 rounded-xl border border-[#cbd8ea] p-4 text-sm leading-6 text-[#233968] focus-within:ring-3 focus-within:ring-[#f4cada]">
        <input
          type="checkbox"
          checked={accepted}
          disabled={submitting}
          onChange={event => onAccepted(event.target.checked)}
          className="mt-0.5 size-5 shrink-0 cursor-pointer accent-reniec-red disabled:cursor-default"
        />
        <span>{review.consentText}</span>
      </label>

      <div className="mt-6 flex flex-col-reverse justify-between gap-3 border-t border-[#e1e8f2] pt-5 sm:flex-row">
        <button type="button" onClick={onBack} disabled={submitting}
          className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-lg px-6 font-bold text-[#173a78] hover:bg-[#f1f5fb] focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df] disabled:cursor-default disabled:opacity-60 sm:w-[280px]">
          <BackIcon /> Regresar
        </button>
        <button type="button" onClick={onConfirm} disabled={!accepted || submitting}
          className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-3 rounded-lg bg-reniec-red px-6 font-bold text-white hover:bg-[#a8003f] focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df] disabled:cursor-default disabled:bg-[#c9cfdb] sm:w-[280px]">
          {submitting ? "Confirmando…" : "Confirmar cancelación"}<ArrowIcon />
        </button>
      </div>
      <p className="sr-only" aria-live="polite">{submitting ? "La confirmación está en proceso." : ""}</p>
    </div>
  );
}

function ConfirmedState({ review }: { review: CancellationReview }) {
  return (
    <div className="grid min-h-[420px] place-items-center text-center" aria-live="polite">
      <div className="max-w-[600px]">
        <span className="mx-auto grid size-16 place-items-center rounded-full bg-[#e8f7ef] text-[#087447]" aria-hidden="true"><CheckIcon /></span>
        <p className="mt-5 text-xs font-black uppercase tracking-[0.14em] text-reniec-red">Confirmación registrada</p>
        <h1 id="review-title" className="mt-3 text-3xl font-black text-[#061a50]">Tu solicitud está preparada</h1>
        <p className="mt-3 leading-7 text-[#52678f]">
          Registramos tu confirmación{review.confirmedAt ? ` el ${formatDate(review.confirmedAt)}` : ""}. Tu solicitud está lista para continuar.
        </p>
      </div>
    </div>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return <div className="grid gap-1 border-b border-[#e2e9f3] px-4 py-3 last:border-0 sm:grid-cols-[180px_1fr]"><dt className="font-bold text-[#0a2259]">{label}</dt><dd className="break-words text-[#425b8e]">{value}</dd></div>;
}

function Data({ label, value, strong = false, mono = false }: { label: string; value: string; strong?: boolean; mono?: boolean }) {
  return <p className="min-w-0"><span className="block text-[11px] font-bold uppercase tracking-wide text-[#7583a4]">{label}</span><span className={`mt-1 block break-all text-sm text-[#314a7d] ${strong ? "font-bold text-[#0a2259]" : ""} ${mono ? "font-mono text-xs" : ""}`}>{value}</span></p>;
}

function LoadingState() {
  return <div className="grid min-h-[420px] place-items-center text-center" aria-live="polite" aria-busy="true"><div><span className="mx-auto block size-10 animate-spin rounded-full border-4 border-[#d6e2f7] border-t-reniec-red motion-reduce:animate-none" /><p className="mt-4 font-semibold text-[#52678f]">Preparando el resumen…</p></div></div>;
}

function ErrorState({ state, onRetry }: { state: Extract<ReviewState, { kind: "error" }>; onRetry: () => void }) {
  return <div className="grid min-h-[390px] place-items-center text-center" role="alert"><div className="max-w-[520px]"><h1 id="review-title" className="text-2xl font-black text-[#061a50]">{state.title}</h1><p className="mt-3 leading-7 text-[#52678f]">{state.description}</p><button type="button" onClick={state.reload ? () => window.location.reload() : onRetry} className="mt-6 min-h-11 cursor-pointer rounded-lg bg-reniec-red px-6 font-bold text-white focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df]">{state.reload ? "Recargar resumen" : "Reintentar"}</button></div></div>;
}

function errorState(error: unknown): Extract<ReviewState, { kind: "error" }> {
  if (error instanceof HttpClientError) {
    if (error.status === 409 || error.code === "CONSENT_VERSION_CHANGED") return { kind: "error", title: "La información fue actualizada", description: "Recarga el resumen y revisa nuevamente la confirmación.", reload: true };
    if (error.status === 403 || error.status === 422) return { kind: "error", title: "Este paso ya no está disponible", description: "La operación cambió y no puede confirmarse con la información actual.", reload: true };
  }
  return { kind: "error", title: "No pudimos preparar la confirmación", description: "Revisa tu conexión e inténtalo nuevamente.", reload: false };
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Fecha no disponible" : dateFormatter.format(date);
}

function BackIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M19 12H5m5-5-5 5 5 5" /></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5" /></svg>; }
function CheckIcon() { return <svg viewBox="0 0 24 24" className="size-9 fill-none stroke-current stroke-[2.5]" aria-hidden="true"><path d="m5 12 4.5 4.5L19 7" /></svg>; }
