import type { ReactNode } from "react";

import { FlowNavigationButton } from "@/components/flow-navigation-button";
import { FlowStepContent } from "@/components/flow-step-content";
import type {
  CancellationExecution,
  CancellationReview,
} from "@/lib/api/cancellation-confirmation";

const formatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "short", timeStyle: "short", hour12: false,
});

export function ReviewView({ dni, review, accepted, submitting, onAccepted, onBack, onConfirm }: {
  dni: string;
  review: CancellationReview;
  accepted: boolean;
  submitting: boolean;
  onAccepted: (value: boolean) => void;
  onBack: () => void;
  onConfirm: () => void;
}) {
  return <FlowStepContent>
    <header className="text-center">
      <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 4 DE 5</p>
      <h1 id="review-title" className="mt-4 text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">Confirma la cancelación</h1>
      <p className="mt-3 text-sm leading-6 text-[#52678f] sm:text-base">Revisa los datos de tu solicitud antes de continuar.</p>
    </header>

    <dl className="mt-7 overflow-hidden rounded-xl bg-white ring-1 ring-[#e5e9f0] shadow-[0_5px_18px_rgba(22,46,91,0.08)]">
      <SummaryRow icon="id" label="DNI" value={dni} />
      <SummaryRow icon="cert" label="Certificado" value={`Orden ${review.certificate.orderNumber} · Creado el ${formatDate(review.certificate.emissionCreatedAt)}`} />
      <SummaryRow icon="shield" label="Motivo" value={review.reasonLabel} />
      <SummaryRow icon="edit" label="Descripción adicional" value={review.otherReason || "No incluye"} />
    </dl>

    <section className="mt-5 rounded-xl bg-[#fffaf2] px-4 py-4 shadow-[0_3px_12px_rgba(128,82,16,0.06)] sm:px-5" aria-labelledby="consequences-title">
      <h2 id="consequences-title" className="flex items-center gap-3 font-black text-[#344360]">
        <span className="grid size-8 place-items-center rounded-full bg-white text-[#e88719] shadow-[0_1px_5px_rgba(139,87,16,0.10)]" aria-hidden="true"><WarningIcon /></span>
        Puntos importantes
      </h2>
      <ul className="mt-2.5 space-y-1.5 pl-3 text-sm leading-6 text-[#52627f]">
        <li className="flex gap-3"><span className="text-[#ef9d3f]" aria-hidden="true">•</span><span>La cancelación se realizará de forma inmediata.</span></li>
        <li className="flex gap-3"><span className="text-[#ef9d3f]" aria-hidden="true">•</span><span>Se cancelará únicamente el certificado digital seleccionado.</span></li>
        <li className="flex gap-3"><span className="text-[#ef9d3f]" aria-hidden="true">•</span><span>Esta acción no afecta tu DNI, tu identidad civil ni tu acceso a ID Perú.</span></li>
      </ul>
    </section>

    <label className="mt-5 flex cursor-pointer items-start gap-4 rounded-xl bg-white px-4 py-4 text-sm leading-6 text-[#42516d] ring-1 ring-[#e3e8f0] shadow-[0_3px_12px_rgba(22,46,91,0.05)] transition-colors hover:ring-[#cfd7e4] sm:px-5">
      <input type="checkbox" checked={accepted} disabled={submitting}
        onChange={event => onAccepted(event.target.checked)}
        className="mt-0.5 size-5 shrink-0 cursor-pointer accent-reniec-red focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df] disabled:cursor-default" />
      <span>He revisado la información y acepto continuar con la cancelación.</span>
    </label>

    <div className="mt-6 flex flex-col-reverse justify-between gap-3 border-t border-[#edf0f5] pt-5 sm:flex-row">
      <FlowNavigationButton variant="secondary" onClick={onBack} disabled={submitting}>
        <BackIcon /> Regresar
      </FlowNavigationButton>
      <FlowNavigationButton variant="primary" onClick={onConfirm} disabled={!accepted || submitting}>
        {submitting ? "Cancelando certificado…" : "Confirmar cancelación"} <ArrowIcon />
      </FlowNavigationButton>
    </div>
    <p className="sr-only" aria-live="polite">{submitting ? "La confirmación está en proceso. No cierres esta ventana." : ""}</p>
  </FlowStepContent>;
}

export function OutcomeView({ outcome, submitting, onRefresh, onRetryReceipt }: {
  outcome: CancellationExecution;
  submitting: boolean;
  onRefresh: () => void;
  onRetryReceipt: () => void;
}) {
  const receiptFailure = outcome.state === "RECEIPT_FAILED";
  const unknown = outcome.state === "OUTCOME_UNKNOWN";
  const failed = outcome.state === "FAILED";
  return <StatusView
    title={receiptFailure ? "La cancelación fue exitosa, pero falta la constancia"
      : unknown ? "Aún no podemos confirmar el resultado"
      : failed ? "No se pudo cancelar el certificado"
      : "Procesando la cancelación"}
    description={receiptFailure ? "El certificado sí fue cancelado. Puedes reintentar únicamente la generación del documento."
      : unknown ? "No vuelvas a iniciar otra cancelación. Consultaremos la misma operación para evitar duplicados."
      : failed ? "El servicio confirmó que el certificado no fue cancelado."
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
function MiniIcon({ kind }: { kind: string }) { const p: Record<string, string> = { id: "M4 6h16v12H4zM7 10h3m-3 3h5m3-4h2m-2 4h2", cert: "M6 3h9l3 3v15H6zM9 10h6m-6 4h4", shield: "M12 3 5 6v5c0 5 3 8 7 10 4-2 7-5 7-10V6l-7-3Zm-3 8 2 2 4-4", edit: "m4 20 4-1 11-11-3-3L5 16l-1 4Z" }; return <svg viewBox="0 0 24 24" className="size-4 fill-none stroke-current stroke-2" aria-hidden="true"><path d={p[kind]} strokeLinecap="round" strokeLinejoin="round"/></svg>; }
function WarningIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v1"/></svg>; }
function WarningLargeIcon() { return <svg viewBox="0 0 24 24" className="size-9 fill-none stroke-current stroke-2" aria-hidden="true"><path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v1"/></svg>; }
function DocumentIcon() { return <svg viewBox="0 0 24 24" className="size-9 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M6 3h9l3 3v15H6zM9 11h6m-6 4h6"/></svg>; }
function BackIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M19 12H5m5-5-5 5 5 5"/></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5"/></svg>; }
