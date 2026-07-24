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
  | { kind: "error"; presentation: ErrorPresentation }
  | { kind: "complete"; data: CertificateList };

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

export function CertificateSelectionTransition({ onBack }: { onBack: () => void }) {
  const [view, setView] = useState<ViewState>({ kind: "loading" });
  const [selected, setSelected] = useState<Set<string>>(() => new Set());
  const [submitting, setSubmitting] = useState(false);
  const submissionInFlight = useRef(false);

  const load = useCallback(async (signal?: AbortSignal) => {
    setView({ kind: "loading" });
    try {
      const result = await getCurrentCertificates(signal);
      if (!result.data) throw new Error("Missing certificate response");
      setSelected(new Set(result.data.certificates.filter(item => item.selected)
        .map(item => item.certificateUuid)));
      if (result.data.requestStatus === "CERTIFICATES_SELECTED") {
        setView({ kind: "complete", data: result.data });
        return;
      }
      if (result.data.certificates.length === 0) {
        setSelected(new Set());
        setView({ kind: "empty" });
        return;
      }
      setView({ kind: "ready", data: result.data });
    } catch (error) {
      if (error instanceof HttpClientError && error.code === "REQUEST_ABORTED") return;
      setView({ kind: "error", presentation: errorPresentation(error) });
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const toggle = (uuid: string) => {
    setSelected(current => {
      const next = new Set(current);
      if (next.has(uuid)) next.delete(uuid);
      else next.add(uuid);
      return next;
    });
  };

  const toggleAll = () => {
    if (view.kind !== "ready") return;
    setSelected(current => current.size === view.data.certificates.length
      ? new Set()
      : new Set(view.data.certificates.map(item => item.certificateUuid)));
  };

  const submit = async () => {
    if (view.kind !== "ready" || selected.size === 0 || submissionInFlight.current) return;
    const currentData = view.data;
    submissionInFlight.current = true;
    setSubmitting(true);
    try {
      const result = await replaceCertificateSelection([...selected]);
      if (!result.data?.canContinue) throw new Error("Selection was not confirmed");
      setView({
        kind: "complete",
        data: {
          ...currentData,
          ...result.data,
          certificates: currentData.certificates.map(certificate => ({
            ...certificate,
            selected: selected.has(certificate.certificateUuid),
          })),
        },
      });
    } catch (error) {
      setView({ kind: "error", presentation: errorPresentation(error) });
    } finally {
      submissionInFlight.current = false;
      setSubmitting(false);
    }
  };

  const returnToSelection = () => {
    if (view.kind !== "complete") return;
    setSelected(new Set(view.data.certificates.filter(item => item.selected)
      .map(item => item.certificateUuid)));
    setView({ kind: "ready", data: view.data });
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
        <CancellationStepper
          currentStep={view.kind === "complete" ? 3 : 2}
          navigableSteps={view.kind === "complete" ? [2] : []}
          onNavigate={view.kind === "complete" ? returnToSelection : undefined}
        />
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
            onToggle={toggle}
            onToggleAll={toggleAll}
            onSubmit={() => void submit()}
            onBack={onBack}
          />
        ) : null}
        {view.kind === "complete" ? <ReasonTransition selectedCount={view.data.selectedCount} /> : null}
      </div>
    </section>
  );
}

export function CertificateSelectionView({ certificates, selected, submitting, onToggle, onToggleAll, onSubmit, onBack }: {
  certificates: CertificateItem[];
  selected: ReadonlySet<string>;
  submitting: boolean;
  onToggle: (uuid: string) => void;
  onToggleAll: () => void;
  onSubmit: () => void;
  onBack: () => void;
}) {
  const allSelected = certificates.length > 0 && selected.size === certificates.length;
  const selectedLabel = `${selected.size} ${selected.size === 1 ? "seleccionado" : "seleccionados"}`;

  return (
    <div className="mx-auto max-w-[920px]">
      <header className="text-center">
        <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">
          PASO 2 DE 5
        </p>
        <h1 id="selection-title" className="mt-4 text-balance text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">
          Elige los certificados que deseas cancelar
        </h1>
        <p className="mx-auto mt-3 max-w-[660px] text-pretty text-sm leading-6 text-[#52678f] sm:text-base">
          Selecciona uno o más certificados digitales vigentes asociados a tu DNI.
        </p>
      </header>

      <div className="mt-7 flex flex-wrap items-end justify-between gap-3 border-b border-[#dce6f5] pb-4">
        <p className="flex items-center gap-2 text-sm font-semibold text-[#233968]">
          <span className="size-2 rounded-full bg-[#16865b]" aria-hidden="true" />
          Certificados vigentes <strong className="text-lg font-black leading-none text-[#061a50]">{certificates.length}</strong>
        </p>
        <p className="text-sm font-bold text-reniec-red" aria-label={selectedLabel} aria-live="polite" aria-atomic="true">
          <span className="mr-1 font-black text-lg leading-none">{selected.size}</span>
          {selected.size === 1 ? "seleccionado" : "seleccionados"}
        </p>
      </div>

      <div className="mt-4 overflow-hidden rounded-xl border border-[#d6e2f3]">
        <div className="hidden grid-cols-[44px_minmax(150px,1fr)_minmax(150px,0.8fr)_minmax(210px,1.15fr)_90px] items-center gap-3 bg-[#f2f6fd] px-4 py-3 text-xs font-black text-[#142b62] md:grid">
          <Checkbox checked={allSelected} label="Seleccionar todos los certificados" onChange={onToggleAll} />
          <span>N.º de orden</span><span>Fecha de emisión</span><span>UUID</span><span>Estado</span>
        </div>
        <div className="divide-y divide-[#e0e8f4]">
          {certificates.map(certificate => (
            <CertificateRow key={certificate.certificateUuid} certificate={certificate}
              checked={selected.has(certificate.certificateUuid)} onToggle={onToggle} />
          ))}
        </div>
      </div>

      <p className="mt-4 flex items-start gap-2 text-sm text-[#52678f]">
        <InfoIcon /> <span>Debes seleccionar al menos un certificado para continuar.</span>
      </p>
      <div className="mt-6 flex flex-col-reverse items-stretch justify-between gap-3 border-t border-[#e1e8f2] pt-5 sm:flex-row sm:items-center">
        <button type="button" onClick={onBack}
          className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-lg px-6 font-bold text-[#173a78] transition-colors hover:bg-[#f1f5fb] focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df] sm:w-[280px]">
          <BackIcon /> Regresar
        </button>
        <button type="button" onClick={onSubmit} disabled={selected.size === 0 || submitting}
          className="inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-3 rounded-lg bg-reniec-red px-6 font-bold text-white transition-colors hover:bg-[#a8003f] focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df] disabled:cursor-default disabled:bg-[#c9cfdb] sm:w-[280px]">
          {submitting ? "Guardando selección…" : "Continuar"}<ArrowIcon />
        </button>
      </div>
    </div>
  );
}

function CertificateRow({ certificate, checked, onToggle }: {
  certificate: CertificateItem; checked: boolean; onToggle: (uuid: string) => void;
}) {
  return (
    <label className={`grid cursor-pointer gap-3 px-4 py-4 transition-colors focus-within:bg-[#f4f8ff] md:grid-cols-[44px_minmax(150px,1fr)_minmax(150px,0.8fr)_minmax(210px,1.15fr)_90px] md:items-center ${checked ? "bg-[#f6f9ff] ring-1 ring-inset ring-[#4f82f5]" : "bg-white hover:bg-[#fbfdff]"}`}>
      <Checkbox checked={checked} label={`Seleccionar certificado ${certificate.orderNumber}`}
        onChange={() => onToggle(certificate.certificateUuid)} />
      <DataCell label="N.º de orden" value={certificate.orderNumber} strong />
      <DataCell label="Fecha de emisión" value={formatDate(certificate.emissionCreatedAt)} />
      <DataCell label="UUID" value={certificate.certificateUuid} mono />
      <span className="w-fit rounded-full bg-[#e3f7ed] px-3 py-1 text-xs font-bold text-[#087447]">Vigente</span>
    </label>
  );
}

function Checkbox({ checked, label, onChange }: { checked: boolean; label: string; onChange: () => void }) {
  return <input type="checkbox" checked={checked} onChange={onChange} aria-label={label}
    className="size-5 cursor-pointer accent-[#0755df] focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df]" />;
}

function DataCell({ label, value, strong = false, mono = false }: { label: string; value: string; strong?: boolean; mono?: boolean }) {
  return <span className="min-w-0"><span className="mb-1 block text-[11px] font-bold uppercase tracking-wide text-[#7583a4] md:hidden">{label}</span><span className={`block break-all text-sm text-[#314a7d] ${strong ? "font-bold text-[#0a2259]" : ""} ${mono ? "font-mono text-xs" : ""}`}>{value}</span></span>;
}

function LoadingState() {
  return <div className="grid min-h-[440px] place-items-center text-center" aria-live="polite" aria-busy="true"><div><span className="mx-auto block size-10 animate-spin rounded-full border-4 border-[#d6e2f7] border-t-reniec-red motion-reduce:animate-none" /><p className="mt-4 font-semibold text-[#52678f]">Consultando tus certificados vigentes…</p></div></div>;
}

function EmptyState({ onExit }: { onExit: () => void }) {
  return <div className="grid min-h-[390px] place-items-center text-center"><div className="max-w-[540px]"><span className="mx-auto grid size-14 place-items-center rounded-full bg-[#edf3ff] text-[#0755df]"><InfoIcon /></span><p className="mt-5 text-xs font-black uppercase tracking-[0.14em] text-reniec-red">Consulta actualizada</p><h1 id="selection-title" className="mt-3 text-2xl font-black text-[#061a50]">Actualmente no existen certificados disponibles</h1><p className="mt-3 leading-7 text-[#52678f]">La disponibilidad cambió desde la consulta inicial, por lo que no es posible continuar con esta operación.</p><button type="button" onClick={onExit} className="mt-6 inline-flex min-h-11 cursor-pointer items-center justify-center rounded-lg border border-[#b9c8df] px-6 font-bold text-[#173a78] focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df]">Volver al inicio</button></div></div>;
}

function ErrorState({ presentation, onRetry }: { presentation: ErrorPresentation; onRetry: () => void }) {
  const href = presentation.action === "home" ? "/" : undefined;
  return <div className="grid min-h-[390px] place-items-center text-center" role="alert"><div className="max-w-[520px]"><h1 id="selection-title" className="text-2xl font-black text-[#061a50]">{presentation.title}</h1><p className="mt-3 leading-7 text-[#52678f]">{presentation.description}</p>{href ? <a href={href} className="mt-6 inline-flex min-h-11 items-center rounded-lg bg-reniec-red px-6 font-bold text-white">Volver al inicio</a> : <button type="button" onClick={presentation.action === "reload" ? () => window.location.reload() : onRetry} className="mt-6 min-h-11 cursor-pointer rounded-lg bg-reniec-red px-6 font-bold text-white focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#0755df]">{presentation.action === "reload" ? "Recargar lista" : "Reintentar"}</button>}</div></div>;
}

function ReasonTransition({ selectedCount }: { selectedCount: number }) {
  return <div className="grid min-h-[390px] place-items-center text-center" aria-live="polite"><div className="max-w-[560px]"><p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 3 DE 5</p><h1 id="selection-title" className="mt-4 text-3xl font-black text-[#061a50]">Selección guardada</h1><p className="mt-3 leading-7 text-[#52678f]">{selectedCount} {selectedCount === 1 ? "certificado quedó seleccionado" : "certificados quedaron seleccionados"}. Ya puedes continuar con el registro del motivo.</p></div></div>;
}

function errorPresentation(error: unknown): ErrorPresentation {
  if (error instanceof HttpClientError) {
    if (error.status === 401 || error.code.startsWith("SESSION_")) return { title: "Tu sesión finalizó", description: "Inicia una nueva operación para continuar.", action: "home" };
    if (error.code === "CERTIFICATE_SELECTION_CONFLICT") return { title: "La selección fue actualizada", description: "Recarga la lista para consultar su estado actual.", action: "reload" };
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
