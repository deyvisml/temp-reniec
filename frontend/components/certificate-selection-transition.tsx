"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { CancellationStepper } from "@/components/cancellation-stepper";
import {
  getCurrentCertificates,
  replaceCertificateSelection,
  type CertificateItem,
  type CertificateList,
} from "@/lib/api/certificate-listing";
import { HttpClientError } from "@/lib/http-client";
import { logoutFlowSession } from "@/lib/api/flow-session";

type ViewState =
  | { kind: "loading" }
  | { kind: "ready"; data: CertificateList }
  | { kind: "empty" }
  | { kind: "error"; presentation: ErrorPresentation };

type ErrorPresentation = {
  title: string;
  description: string;
  action: "retry" | "reload" | "home";
};

const dateFormatter = new Intl.DateTimeFormat("es-PE", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

export function CertificateSelectionTransition({ onBack, onContinue }: { onBack?: () => void; onContinue?: () => void } = {}) {
  const [view, setView] = useState<ViewState>({ kind: "loading" });
  const [selected, setSelected] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submissionInFlight = useRef(false);

  const load = useCallback(async (signal?: AbortSignal): Promise<void> => {
    setView({ kind: "loading" });
    try {
      const result = await getCurrentCertificates();
      if (signal?.aborted) return;
      if (!result.data) throw new Error("Missing certificate response");
      setSelected(result.data.certificates.find(item => item.selected)?.certificateUuid ?? null);
      if (result.data.certificates.length === 0) {
        setSelected(null);
        setView({ kind: "empty" });
        return;
      }
      setView({ kind: "ready", data: result.data });
    } catch (error) {
      if (signal?.aborted || (error instanceof HttpClientError && error.code === "REQUEST_ABORTED")) return;
      setView({ kind: "error", presentation: errorPresentation(error) });
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const submit = async () => {
    if (view.kind !== "ready" || selected === null || submissionInFlight.current) return;
    submissionInFlight.current = true;
    setSubmitting(true);
    try {
      const result = await replaceCertificateSelection(selected);
      if (!result.data?.canContinue) throw new Error("Selection was not confirmed");
      onContinue?.();
    } catch (error) {
      setView({ kind: "error", presentation: errorPresentation(error) });
    } finally {
      submissionInFlight.current = false;
      setSubmitting(false);
    }
  };

  const exitToHome = async () => {
    try {
      await logoutFlowSession();
    } finally {
      window.location.assign("/");
    }
  };

  return (
    <section className="mx-auto w-full max-w-[1040px]" aria-labelledby="selection-title">
      <div className="px-2 sm:px-8 lg:px-14">
        <CancellationStepper currentStep={2} />
      </div>
      <div className="mx-2 mt-6 rounded-[22px] border border-[#dfe7f3] bg-white/95 px-4 py-7 sm:mx-8 sm:px-8 sm:py-9 lg:mx-14">
        {view.kind === "loading" ? <LoadingState /> : null}
        {view.kind === "empty" ? <EmptyState onExit={() => void exitToHome()} /> : null}
        {view.kind === "error" ? (
          <ErrorState presentation={view.presentation} onRetry={() => void load()} />
        ) : null}
        {view.kind === "ready" ? (
          <CertificateSelectionView
            certificates={view.data.certificates}
            selected={selected}
            submitting={submitting}
            onSelect={setSelected}
            onSubmit={() => void submit()}
            onBack={onBack}
          />
        ) : null}
      </div>
    </section>
  );
}

export function CertificateSelectionView({ certificates, selected, submitting, onSelect, onSubmit, onBack }: {
  certificates: CertificateItem[];
  selected: string | null;
  submitting: boolean;
  onSelect: (uuid: string) => void;
  onSubmit: () => void;
  onBack?: () => void;
}) {
  const selectedLabel = selected ? "1 certificado seleccionado" : "Ningún certificado seleccionado";

  return (
    <div className="mx-auto max-w-[920px]">
      <header className="text-center">
        <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">
          PASO 2 DE 5
        </p>
        <h1 id="selection-title" className="mt-4 text-balance text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">
          Selecciona un certificado
        </h1>
        <p className="mx-auto mt-3 max-w-[660px] text-pretty text-sm leading-6 text-[#52678f] sm:text-base">
          Elige cuál deseas cancelar para continuar.
        </p>
      </header>

      <div className="mt-7 flex flex-wrap items-end justify-between gap-3 border-b border-[#dce6f5] pb-4">
        <p className="flex items-center gap-2 text-sm font-semibold text-[#233968]">
          <span className="size-2 rounded-full bg-[#16865b]" aria-hidden="true" />
          Certificados vigentes <strong className="text-lg font-black leading-none text-[#061a50]">{certificates.length}</strong>
        </p>
        <p className="text-sm font-bold text-reniec-red" aria-live="polite" aria-atomic="true">
          {selectedLabel}
        </p>
      </div>

      <fieldset className="mt-4 space-y-3" aria-describedby="certificate-selection-help">
        <legend className="sr-only">Certificados digitales vigentes</legend>
        <div className="space-y-3">
          {certificates.map((certificate, index) => (
            <CertificateRow key={certificate.certificateUuid} certificate={certificate}
              position={index + 1} checked={selected === certificate.certificateUuid} onSelect={onSelect} />
          ))}
        </div>
      </fieldset>

      <p id="certificate-selection-help" className="mt-4 flex items-start gap-2 text-sm text-[#52678f]">
        <InfoIcon /> <span>Debes seleccionar un certificado para continuar.</span>
      </p>
      <div className="mt-6 flex flex-col-reverse items-stretch justify-between gap-3 border-t border-[#e1e8f2] pt-5 sm:flex-row sm:items-center">
        {onBack ? <button type="button" onClick={onBack}
          className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-lg px-6 font-bold text-[#173a78] transition-colors hover:bg-[#f1f5fb] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df] sm:w-[280px]">
          <BackIcon /> Regresar
        </button> : <span aria-hidden="true" />}
        <button type="button" onClick={onSubmit} disabled={selected === null || submitting}
          className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-3 rounded-lg bg-reniec-red px-6 font-bold text-white transition-colors hover:bg-[#a8003f] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df] disabled:cursor-default disabled:bg-[#c9cfdb] sm:w-[280px]">
          {submitting ? "Guardando selección…" : "Continuar"}<ArrowIcon />
        </button>
      </div>
    </div>
  );
}

function CertificateRow({ certificate, position, checked, onSelect }: {
  certificate: CertificateItem; position: number; checked: boolean; onSelect: (uuid: string) => void;
}) {
  const visibleName = `Certificado digital vigente ${String(position).padStart(2, "0")}`;

  return (
    <label
      data-selected={checked ? "true" : "false"}
      className={`grid cursor-pointer grid-cols-[28px_56px_minmax(0,1fr)] items-start gap-x-3 gap-y-3 rounded-xl border px-4 py-4 transition-[border-color,background-color] duration-200 focus-within:ring-1 focus-within:ring-inset focus-within:ring-[#0755df] md:grid-cols-[32px_64px_minmax(0,1fr)_110px] md:items-center md:gap-x-4 md:px-5 ${checked ? "border-[#1768f2] bg-[#f5f8ff]" : "border-[#d6e2f3] bg-white hover:border-[#8eafe9] hover:bg-[#fbfdff]"}`}
    >
      <input type="radio" name="selected-certificate" value={certificate.certificateUuid}
        checked={checked} onChange={() => onSelect(certificate.certificateUuid)}
        aria-label={`Seleccionar certificado ${certificate.orderNumber}`}
        className="sr-only" />

      <span
        aria-hidden="true"
        className={`mt-3 grid size-7 shrink-0 place-items-center rounded-full border-2 transition-colors duration-200 md:mt-0 ${checked ? "border-[#1768f2] bg-[#1768f2] text-white" : "border-[#aab5c8] bg-white text-transparent"}`}
      >
        <CheckIcon />
      </span>

      <span
        aria-hidden="true"
        className="grid size-14 place-items-center rounded-lg border border-[#dce6f6] bg-[#f7f9fd] text-[#174ea6] md:size-16"
      >
        <CertificateIcon />
      </span>

      <span className="col-span-3 min-w-0 md:col-span-1">
        <span className="block text-base font-black leading-6 text-[#061a50] md:text-lg">
          {visibleName}
        </span>
        <span className="mt-3 grid min-w-0 gap-3 sm:grid-cols-2 sm:gap-0">
          <DataCell label="Orden" value={certificate.orderNumber} strong />
          <DataCell label="Creado el" value={formatDate(certificate.emissionCreatedAt)} />
        </span>
      </span>

      <span className="col-start-3 row-start-1 mt-1 w-fit rounded-full bg-[#e3f7ed] px-3.5 py-1.5 text-xs font-bold text-[#087447] md:col-start-4 md:row-auto md:mt-0 md:justify-self-end">
        ✓ Vigente
      </span>
    </label>
  );
}

function DataCell({ label, value, strong = false }: { label: string; value: string; strong?: boolean }) {
  return (
    <span className="min-w-0 sm:px-4 sm:first:pl-0 sm:last:pr-0 sm:[&+span]:border-l sm:[&+span]:border-[#dce6f5]">
      <span className="mb-1 block text-xs font-semibold text-[#52678f]">{label}</span>
      <span
        title={value}
        className={`block min-w-0 truncate text-sm text-[#314a7d] ${strong ? "font-bold text-[#0a2259]" : ""}`}
      >
        {value}
      </span>
    </span>
  );
}

function LoadingState() {
  return <div className="grid min-h-[440px] place-items-center text-center" aria-live="polite" aria-busy="true"><div><span className="mx-auto block size-10 animate-spin rounded-full border-4 border-[#d6e2f7] border-t-reniec-red motion-reduce:animate-none" /><p className="mt-4 font-semibold text-[#52678f]">Consultando tus certificados vigentes…</p></div></div>;
}

function EmptyState({ onExit }: { onExit: () => void }) {
  return <div className="grid min-h-[390px] place-items-center text-center"><div className="max-w-[540px]"><span className="mx-auto grid size-14 place-items-center rounded-full bg-[#edf3ff] text-[#0755df]"><InfoIcon /></span><p className="mt-5 text-xs font-black uppercase tracking-[0.14em] text-reniec-red">Consulta actualizada</p><h1 id="selection-title" className="mt-3 text-2xl font-black text-[#061a50]">Actualmente no existen certificados disponibles</h1><p className="mt-3 leading-7 text-[#52678f]">La disponibilidad cambió desde la consulta inicial, por lo que no es posible continuar con esta operación.</p><button type="button" onClick={onExit} className="mt-6 inline-flex min-h-11 cursor-pointer items-center justify-center rounded-lg border border-[#b9c8df] px-6 font-bold text-[#173a78] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df]">Volver al inicio</button></div></div>;
}

function ErrorState({ presentation, onRetry }: { presentation: ErrorPresentation; onRetry: () => void }) {
  const href = presentation.action === "home" ? "/" : undefined;
  return <div className="grid min-h-[390px] place-items-center text-center" role="alert"><div className="max-w-[520px]"><h1 id="selection-title" className="text-2xl font-black text-[#061a50]">{presentation.title}</h1><p className="mt-3 leading-7 text-[#52678f]">{presentation.description}</p>{href ? <a href={href} className="mt-6 inline-flex min-h-11 items-center rounded-lg bg-reniec-red px-6 font-bold text-white">Volver al inicio</a> : <button type="button" onClick={presentation.action === "reload" ? () => window.location.reload() : onRetry} className="mt-6 min-h-11 cursor-pointer rounded-lg bg-reniec-red px-6 font-bold text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df]">{presentation.action === "reload" ? "Recargar lista" : "Reintentar"}</button>}</div></div>;
}

function errorPresentation(error: unknown): ErrorPresentation {
  if (error instanceof HttpClientError) {
    if (error.status === 401 || error.code.startsWith("SESSION_")) return { title: "Tu sesión finalizó", description: "Inicia una nueva operación para continuar.", action: "home" };
    if (error.code === "CERTIFICATE_SELECTION_CONFLICT") return { title: "La selección fue actualizada", description: "Recarga la lista para consultar su estado actual.", action: "reload" };
    if (error.code === "CERTIFICATE_LIST_IN_PROGRESS") return { title: "Estamos consultando tus certificados", description: "La consulta continúa en proceso. Espera un momento e inténtalo nuevamente.", action: "retry" };
    if (error.code === "CERTIFICATE_LIST_INVALID_RESPONSE") return { title: "No pudimos interpretar la lista", description: "La respuesta recibida no fue válida. Puedes intentarlo nuevamente.", action: "retry" };
    if (error.code === "CERTIFICATE_LIST_TIMEOUT") return { title: "La consulta tardó demasiado", description: "No se realizaron cambios. Puedes intentarlo nuevamente.", action: "retry" };
    if (error.code === "CERTIFICATE_LIST_UNAVAILABLE") return { title: "Servicio temporalmente no disponible", description: "No podemos consultar tus certificados en este momento.", action: "retry" };
  }
  return { title: "No pudimos completar la operación", description: "Revisa tu conexión e inténtalo nuevamente.", action: "retry" };
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Fecha no disponible" : dateFormatter.format(date);
}

function InfoIcon() { return <svg viewBox="0 0 24 24" className="mt-0.5 size-5 shrink-0 fill-none stroke-current stroke-2" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5h.01"/></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5"/></svg>; }
function BackIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M19 12H5m5-5-5 5 5 5"/></svg>; }
function CheckIcon() { return <svg viewBox="0 0 20 20" className="size-4 fill-none stroke-current stroke-[2.5]" aria-hidden="true"><path d="m4.5 10 3.2 3.2 7.8-7.8" /></svg>; }
function CertificateIcon() {
  return (
    <svg viewBox="0 0 56 64" className="h-12 w-11 fill-none stroke-current md:h-14 md:w-12" aria-hidden="true">
      <path d="M10 3h25l11 11v34H10z" strokeWidth="1.8" />
      <path d="M35 3v12h11M17 20h20M17 27h20M17 34h12" strokeWidth="1.8" />
      <circle cx="36" cy="42" r="7" fill="#f7f9fd" strokeWidth="1.8" />
      <path d="m32.5 48-1.2 11 4.7-3 4.7 3-1.2-11M33.5 42l1.6 1.6 3.5-3.5" strokeWidth="1.8" />
    </svg>
  );
}
