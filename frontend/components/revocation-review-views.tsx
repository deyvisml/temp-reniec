"use client";

import { useEffect, useMemo, useState, type ReactNode } from "react";

import { FlowNavigationButton } from "@/components/flow-navigation-button";
import { FlowStepContent } from "@/components/flow-step-content";
import type {
  RevocationExecution,
  RevocationReview,
} from "@/lib/api/revocation-confirmation";

const formatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "short", timeStyle: "short", hour12: false,
});

export function ReviewView({ dni, review, accepted, submitting, onAccepted, onBack, onConfirm }: {
  dni: string;
  review: RevocationReview;
  accepted: boolean;
  submitting: boolean;
  onAccepted: (value: boolean) => void;
  onBack: () => void;
  onConfirm: () => void;
}) {
  return <FlowStepContent>
    <header className="text-center">
      <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 4 DE 5</p>
      <h1 id="review-title" className="mt-4 text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">Confirma la revocación</h1>
      <p className="mt-3 text-sm leading-6 text-[#52678f] sm:text-base">Revisa los datos de tu solicitud antes de continuar.</p>
    </header>

    <dl className="mt-7 overflow-hidden rounded-xl bg-white ring-1 ring-[#e5e9f0] shadow-[0_5px_18px_rgba(22,46,91,0.08)]">
      <SummaryRow icon="id" label="DNI" value={dni} />
      {review.firstName ? <SummaryRow icon="person" label="Nombre" value={review.firstName} /> : null}
      <SummaryRow icon="cert" label="Credencial" value={`Índice ${review.digitalCredential.statusListIndex} · Creado el ${formatDate(review.digitalCredential.emissionCreatedAt)}`} />
      <SummaryRow icon="shield" label="Motivo" value={review.reasonLabel} />
      <SummaryRow icon="edit" label="Descripción adicional" value={review.otherReason || "No incluye"} />
    </dl>

    <section className="mt-5 rounded-xl bg-[#fffaf2] px-4 py-4 shadow-[0_3px_12px_rgba(128,82,16,0.06)] sm:px-5" aria-labelledby="consequences-title">
      <h2 id="consequences-title" className="flex items-center gap-3 font-black text-[#344360]">
        <span className="grid size-8 place-items-center rounded-full bg-white text-[#e88719] shadow-[0_1px_5px_rgba(139,87,16,0.10)]" aria-hidden="true"><WarningIcon /></span>
        Puntos importantes
      </h2>
      <ul className="mt-2.5 space-y-1.5 pl-3 text-sm leading-6 text-[#52627f]">
        <li className="flex gap-3"><span className="text-[#ef9d3f]" aria-hidden="true">•</span><span>La propagación de la revocación puede tardar aproximadamente un minuto.</span></li>
        <li className="flex gap-3"><span className="text-[#ef9d3f]" aria-hidden="true">•</span><span>Se revocará únicamente la credencial digital seleccionada.</span></li>
        <li className="flex gap-3"><span className="text-[#ef9d3f]" aria-hidden="true">•</span><span>Esta acción no afecta tu DNI, tu identidad civil ni tu acceso a ID Perú.</span></li>
      </ul>
    </section>

    <label className="mt-5 flex cursor-pointer items-start gap-4 rounded-xl bg-white px-4 py-4 text-sm leading-6 text-[#42516d] ring-1 ring-[#e3e8f0] shadow-[0_3px_12px_rgba(22,46,91,0.05)] transition-colors hover:ring-[#cfd7e4] sm:px-5">
      <input type="checkbox" checked={accepted} disabled={submitting}
        onChange={event => onAccepted(event.target.checked)}
        className="mt-0.5 size-5 shrink-0 cursor-pointer accent-reniec-red focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df] disabled:cursor-default" />
      <span>He revisado la información y acepto continuar con la revocación.</span>
    </label>

    <div className="mt-6 flex flex-col-reverse justify-between gap-3 border-t border-[#edf0f5] pt-5 sm:flex-row">
      <FlowNavigationButton variant="secondary" onClick={onBack} disabled={submitting}>
        <BackIcon /> Regresar
      </FlowNavigationButton>
      <FlowNavigationButton variant="primary" onClick={onConfirm} disabled={!accepted || submitting}>
        {submitting ? "Revocando credencial…" : "Confirmar revocación"} <ArrowIcon />
      </FlowNavigationButton>
    </div>
    <p className="sr-only" aria-live="polite">{submitting ? "La confirmación está en proceso. No cierres esta ventana." : ""}</p>
  </FlowStepContent>;
}

export function ProcessingView({ outcome }: { outcome: RevocationExecution }) {
  const processing = outcome.processing;
  const clockOffset = useMemo(() => processing
    ? new Date(processing.serverTime).getTime() - Date.now()
    : 0, [processing]);
  const [now, setNow] = useState(() => Date.now() + clockOffset);

  useEffect(() => {
    setNow(Date.now() + clockOffset);
    const timer = window.setInterval(() => setNow(Date.now() + clockOffset), 1_000);
    return () => window.clearInterval(timer);
  }, [clockOffset]);

  const startedAt = processing ? new Date(processing.startedAt).getTime() : Number.NaN;
  const readyAt = processing?.readyAt ? new Date(processing.readyAt).getTime() : Number.NaN;
  const hasPropagationWindow = Number.isFinite(startedAt) && Number.isFinite(readyAt) && readyAt > startedAt;
  const remainingSeconds = hasPropagationWindow ? Math.max(0, Math.ceil((readyAt - now) / 1_000)) : 0;
  const remainingTime = formatRemainingTime(remainingSeconds);
  const progress = hasPropagationWindow
    ? Math.min(100, Math.max(0, Math.round(((now - startedAt) / (readyAt - startedAt)) * 100)))
    : 0;
  const propagating = processing?.phase === "PROPAGATING" && remainingSeconds > 0;

  return <div className="grid min-h-[330px] place-items-center px-2 py-4 text-center" aria-busy="true">
    <div className="w-full max-w-[480px]">
      <PropagationIndicator />
      <h1 id="review-title" className="mt-5 text-2xl font-bold tracking-[-0.02em] text-[#061a50]">
        {propagating ? "Completando la revocación" : "Preparando tu constancia"}
      </h1>
      <p className="mx-auto mt-2.5 max-w-[430px] text-base leading-6 text-[#52678f]">
        {propagating
          ? "Este proceso puede tardar aproximadamente un minuto."
          : "Ya casi terminamos."}
      </p>

      {propagating ? <div className="mx-auto mt-7 max-w-[420px]">
        <progress
          value={progress}
          max={100}
          role="progressbar"
          aria-label="Progreso de propagación de la revocación"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={progress}
          aria-valuetext={`${remainingSeconds} segundos restantes estimados`}
          className="block h-2 w-full appearance-none overflow-hidden rounded-full border-0 bg-[#e7edf7] [&::-moz-progress-bar]:rounded-full [&::-moz-progress-bar]:bg-[#1768f2] [&::-webkit-progress-bar]:rounded-full [&::-webkit-progress-bar]:bg-[#e7edf7] [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-[#1768f2]"
        />
        <div className="mt-3 flex items-center justify-between gap-4 text-sm text-[#52678f]" aria-hidden="true">
          <span>Tiempo estimado restante</span>
          <time
            dateTime={`PT${remainingSeconds}S`}
            className="font-bold tabular-nums tracking-[0.04em] text-[#183b70]"
          >
            {remainingTime}
          </time>
        </div>
      </div> : <div
        className="mx-auto mt-7 max-w-[420px] overflow-hidden rounded-full bg-[#e7edf7]"
        role="progressbar"
        aria-label="Preparando la constancia"
        aria-valuetext="Generación en curso"
      >
        <span className="block h-2 w-1/3 animate-[receipt-progress_1.4s_ease-in-out_infinite] rounded-full bg-[#1768f2] motion-reduce:mx-auto motion-reduce:animate-none" />
      </div>}

      <p className="sr-only" role="status" aria-live="polite">
        {propagating ? "La revocación se está completando." : "La constancia se está preparando."}
      </p>
    </div>
  </div>;
}

function formatRemainingTime(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export function OutcomeView({ outcome, submitting, onRefresh, onRetryReceipt }: {
  outcome: RevocationExecution;
  submitting: boolean;
  onRefresh: () => void;
  onRetryReceipt: () => void;
}) {
  const receiptFailure = outcome.state === "RECEIPT_FAILED";
  const unknown = outcome.state === "OUTCOME_UNKNOWN";
  const failed = outcome.state === "FAILED";
  return <StatusView
    title={receiptFailure ? "La revocación fue exitosa, pero falta la constancia"
      : unknown ? "Aún no podemos confirmar el resultado"
      : failed ? "No se pudo revocar la credencial"
      : "Procesando la revocación"}
    description={receiptFailure ? "La credencial sí fue revocada. Puedes reintentar únicamente la generación del documento."
      : unknown ? "No vuelvas a iniciar otra revocación. Consultaremos la misma operación para evitar duplicados."
      : failed ? "El servicio confirmó que la credencial no fue revocada."
      : "Estamos procesando la misma operación de forma segura."}
    documentIcon={receiptFailure}
  >
    {receiptFailure ? <button type="button" onClick={onRetryReceipt} disabled={submitting} className="rounded-lg bg-reniec-red px-6 py-3 font-bold text-white disabled:opacity-60">{submitting ? "Generando…" : "Generar constancia nuevamente"}</button> : null}
    {unknown || outcome.state === "PROCESSING" ? <button type="button" onClick={onRefresh} disabled={submitting} className="rounded-lg bg-reniec-red px-6 py-3 font-bold text-white disabled:opacity-60">{submitting ? "Consultando…" : "Consultar estado"}</button> : null}
  </StatusView>;
}

export function SubmissionUncertainView({ submitting, onRetry }: {
  submitting: boolean;
  onRetry: () => void;
}) {
  return <StatusView
    title="Aún no podemos confirmar el resultado"
    description="La comunicación se interrumpió después de enviar la solicitud. Reintentaremos exactamente la misma operación para consultar su resultado sin duplicarla."
  >
    <button type="button" onClick={onRetry} disabled={submitting}
      className="rounded-lg bg-reniec-red px-6 py-3 font-bold text-white disabled:opacity-60">
      {submitting ? "Consultando…" : "Consultar estado"}
    </button>
  </StatusView>;
}

function StatusView({ title, description, documentIcon = false, children }: {
  title: string;
  description: string;
  documentIcon?: boolean;
  children: ReactNode;
}) {
  return <div className="grid min-h-[430px] place-items-center text-center" aria-live="polite">
    <div className="max-w-[610px]">
      <span className={`mx-auto grid size-16 place-items-center rounded-full ${documentIcon ? "bg-[#fff5df] text-[#a25e00]" : "bg-[#fff0f3] text-[#a8003f]"}`} aria-hidden="true">
        {documentIcon ? <DocumentIcon /> : <WarningLargeIcon />}
      </span>
      <h1 id="review-title" className="mt-5 text-2xl font-black text-[#061a50] sm:text-3xl">{title}</h1>
      <p className="mt-3 leading-7 text-[#52678f]">{description}</p>
      <div className="mt-6">{children}</div>
    </div>
  </div>;
}

function SummaryRow({ icon, label, value }: { icon: string; label: string; value: string }) {
  return <div className="grid grid-cols-[36px_1fr] items-center gap-x-3 border-b border-[#edf0f5] px-4 py-3.5 last:border-0 sm:grid-cols-[36px_190px_1fr] sm:px-5">
    <span className="grid size-8 place-items-center rounded-lg bg-[#fff0f5] text-reniec-red" aria-hidden="true"><MiniIcon kind={icon} /></span>
    <dt className="font-bold text-[#0a2259]">{label}</dt>
    <dd className="col-start-2 mt-1 break-words text-[#425b8e] sm:col-start-3 sm:mt-0">{value}</dd>
  </div>;
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Fecha no disponible" : formatter.format(date);
}
function MiniIcon({ kind }: { kind: string }) { const p: Record<string, string> = { id: "M4 6h16v12H4zM7 10h3m-3 3h5m3-4h2m-2 4h2", person: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm7 8a7 7 0 0 0-14 0", cert: "M6 3h9l3 3v15H6zM9 10h6m-6 4h4", shield: "M12 3 5 6v5c0 5 3 8 7 10 4-2 7-5 7-10V6l-7-3Zm-3 8 2 2 4-4", edit: "m4 20 4-1 11-11-3-3L5 16l-1 4Z" }; return <svg viewBox="0 0 24 24" className="size-4 fill-none stroke-current stroke-2" aria-hidden="true"><path d={p[kind]} strokeLinecap="round" strokeLinejoin="round"/></svg>; }
function WarningIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v1"/></svg>; }
function WarningLargeIcon() { return <svg viewBox="0 0 24 24" className="size-9 fill-none stroke-current stroke-2" aria-hidden="true"><path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v1"/></svg>; }
function DocumentIcon() { return <svg viewBox="0 0 24 24" className="size-9 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M6 3h9l3 3v15H6zM9 11h6m-6 4h6"/></svg>; }
function PropagationIndicator() {
  return <span className="relative mx-auto block size-24" aria-hidden="true">
    <span className="absolute inset-3 rounded-full border border-[#1768f2]/35 animate-[propagation-wave_2.4s_ease-out_infinite] motion-reduce:animate-none" />
    <span className="absolute inset-3 rounded-full border border-[#1768f2]/35 animate-[propagation-wave_2.4s_ease-out_infinite] [animation-delay:-1.2s] motion-reduce:animate-none" />
    <svg viewBox="0 0 96 96" className="relative size-full fill-none" focusable="false">
      <circle cx="48" cy="48" r="25" fill="#edf3ff" />
      <circle cx="48" cy="48" r="7" fill="#1768f2" />
      <circle cx="48" cy="48" r="3" fill="white" />
    </svg>
  </span>;
}
function BackIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M19 12H5m5-5-5 5 5 5"/></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5"/></svg>; }
