"use client";

import { useCallback, useEffect, useState } from "react";

import { RevocationStepper } from "@/components/revocation-stepper";
import { FlowNavigationButton } from "@/components/flow-navigation-button";
import { FlowStepContent } from "@/components/flow-step-content";
import {
  getCurrentDigitalCredentials,
  type DigitalCredentialItem,
  type DigitalCredentialList,
} from "@/lib/api/digital-credential-listing";
import { HttpClientError } from "@/lib/http-client";
import { logoutFlowSession } from "@/lib/api/flow-session";

type ViewState =
  | { kind: "loading" }
  | { kind: "ready"; data: DigitalCredentialList }
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

export type DigitalCredentialSelection = Pick<
  DigitalCredentialItem,
  "digitalCredentialUuid" | "statusListIndex"
>;

const isSelected = (
  credential: DigitalCredentialItem,
  selected: DigitalCredentialSelection | null,
) => credential.digitalCredentialUuid === selected?.digitalCredentialUuid
  && credential.statusListIndex === selected.statusListIndex;

const credentialKey = (credential: DigitalCredentialSelection) =>
  `${credential.digitalCredentialUuid}:${credential.statusListIndex}`;

export function DigitalCredentialSelectionTransition({ selectionStale = false, selected, onSelect, onBack, onContinue }: {
	selectionStale?: boolean;
  selected: DigitalCredentialSelection | null;
  onSelect: (selection: DigitalCredentialSelection) => void;
  onBack?: () => void;
  onContinue?: () => void;
}) {
  const [view, setView] = useState<ViewState>({ kind: "loading" });

  const load = useCallback(async (signal?: AbortSignal): Promise<void> => {
    setView({ kind: "loading" });
    try {
      const result = await getCurrentDigitalCredentials();
      if (signal?.aborted) return;
      if (!result.data) throw new Error("Missing digitalCredential response");
      if (result.data.digitalCredentials.length === 0) {
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
    if (view.kind !== "ready" || selected === null) return;
    const selectedCredential = view.data.digitalCredentials.find(
      (credential) => isSelected(credential, selected) && credential.status === "ACTIVE",
    );
    if (!selectedCredential) return;
    onContinue?.();
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
        <RevocationStepper currentStep={2} />
      </div>
      <div className="mx-2 mt-6 rounded-2xl bg-white px-4 py-7 sm:mx-8 sm:px-8 sm:py-9 lg:mx-14">
		{selectionStale ? (
			<div className="mb-6 rounded-lg border border-[#e6b94f] bg-[#fff8e6] px-4 py-3 text-sm text-[#644b12]" role="alert">
				La credencial seleccionada ya no está vigente. Revisa la lista actualizada y selecciona otra para continuar.
			</div>
		) : null}
        {view.kind === "loading" ? <LoadingState /> : null}
        {view.kind === "empty" ? <EmptyState onExit={() => void exitToHome()} /> : null}
        {view.kind === "error" ? (
          <ErrorState presentation={view.presentation} onRetry={() => void load()} />
        ) : null}
        {view.kind === "ready" ? (
          <DigitalCredentialSelectionView
            digitalCredentials={view.data.digitalCredentials}
            selected={view.data.digitalCredentials.some(
              (credential) => isSelected(credential, selected) && credential.status === "ACTIVE",
            ) ? selected : null}
            submitting={false}
            onSelect={onSelect}
            onSubmit={() => void submit()}
            onBack={onBack}
          />
        ) : null}
      </div>
    </section>
  );
}

export function DigitalCredentialSelectionView({ digitalCredentials, selected, submitting, onSelect, onSubmit, onBack }: {
  digitalCredentials: DigitalCredentialItem[];
  selected: DigitalCredentialSelection | null;
  submitting: boolean;
  onSelect: (selection: DigitalCredentialSelection) => void;
  onSubmit: () => void;
  onBack?: () => void;
}) {
  const activeCredentials = digitalCredentials.filter((credential) => credential.status === "ACTIVE");
  const revokedCredentials = digitalCredentials.filter((credential) => credential.status === "REVOKED");
  const hasValidSelection = activeCredentials.some(
    (credential) => isSelected(credential, selected),
  );
  const selectedLabel = hasValidSelection ? "1 credencial seleccionada" : "Ninguna credencial seleccionada";
  const selectionStatusColor = hasValidSelection ? "text-[#1768f2]" : "text-[#52678f]";

  return (
    <FlowStepContent>
      <header className="text-center">
        <h1 id="selection-title" className="text-balance text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">
          Selecciona una credencial
        </h1>
        <p className="mx-auto mt-2 max-w-[660px] text-pretty text-sm leading-6 text-[#52678f] sm:text-base">
          Elige cuál deseas revocar para continuar.
        </p>
      </header>

      <div className="mt-7 flex flex-col gap-3 rounded-lg bg-[#f8f9fb] px-4 py-3.5 ring-1 ring-[#e5e9f0] sm:flex-row sm:items-center sm:justify-between sm:px-5">
        <p className="flex items-center gap-3 text-sm font-medium text-[#243654]">
          <span className="grid size-8 place-items-center rounded-full bg-white text-[#314a73] ring-1 ring-[#e1e6ed]" aria-hidden="true"><DigitalCredentialListIcon /></span>
          <span>Credenciales verificables</span>
          <span className="text-lg font-medium leading-none text-[#061a50]">{digitalCredentials.length}</span>
          <span className="hidden text-xs font-normal text-[#687893] sm:inline">
            {activeCredentials.length} vigentes · {revokedCredentials.length} revocadas
          </span>
        </p>
        <p className={`flex items-center gap-2 border-t border-[#e1e5eb] pt-3 text-sm font-medium sm:border-t-0 sm:border-l sm:py-1 sm:pl-5 ${selectionStatusColor}`} aria-live="polite" aria-atomic="true">
          <InfoIcon /> <span>{selectedLabel}</span>
        </p>
      </div>

      <fieldset className="mt-6" aria-describedby="digital-credential-selection-help">
        <legend className="mb-3 flex w-full items-center justify-between text-sm font-semibold text-[#243654]">
          <span>Disponibles para revocar</span>
          <span className="rounded-full bg-[#edf3ff] px-2.5 py-1 text-xs font-medium text-[#315b9e]">
            {activeCredentials.length}
          </span>
        </legend>
        <div className="mb-2 hidden grid-cols-[64px_minmax(0,1fr)_160px_96px] gap-x-4 px-5 text-[11px] font-semibold uppercase text-[#687893] md:grid">
          <span>Seleccionar</span>
          <span>Credencial</span>
          <span>Fecha de creación</span>
          <span className="text-center">Estado</span>
        </div>
        <div className="space-y-3">
          {activeCredentials.map((digitalCredential, index) => (
            <DigitalCredentialRow key={credentialKey(digitalCredential)} digitalCredential={digitalCredential}
              position={index + 1} checked={isSelected(digitalCredential, selected)} onSelect={onSelect} />
          ))}
        </div>
      </fieldset>

      {revokedCredentials.length > 0 ? (
        <section className="mt-8 border-t border-[#e6ebf2] pt-6" aria-labelledby="revoked-digital-credentials-title">
          <div className="mb-3 flex items-center justify-between">
            <div>
              <h2 id="revoked-digital-credentials-title" className="text-sm font-semibold text-[#243654]">
                Credenciales revocadas
              </h2>
              <p className="mt-1 text-xs leading-5 text-[#687893]">
                Se muestran únicamente como historial y no pueden volver a seleccionarse.
              </p>
            </div>
            <span className="rounded-full bg-[#f0f2f5] px-2.5 py-1 text-xs font-medium text-[#68717f]">
              {revokedCredentials.length}
            </span>
          </div>
          <div className="mb-2 hidden grid-cols-[64px_minmax(0,1fr)_160px_96px] gap-x-4 px-5 text-[11px] font-semibold uppercase text-[#7b8492] md:grid">
            <span aria-hidden="true" />
            <span>Credencial</span>
            <span>Fechas</span>
            <span className="text-center">Estado</span>
          </div>
          <div className="space-y-3">
            {revokedCredentials.map((digitalCredential, index) => (
              <RevokedDigitalCredentialRow
                key={credentialKey(digitalCredential)}
                digitalCredential={digitalCredential}
                position={index + 1}
              />
            ))}
          </div>
        </section>
      ) : null}

      <div className="mt-7 flex flex-col-reverse items-stretch justify-between gap-4 border-t border-[#e1e8f2] pt-5 sm:flex-row sm:items-center">
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center">
          {onBack ? <FlowNavigationButton variant="secondary" onClick={onBack}>
            <BackIcon /> Regresar
          </FlowNavigationButton> : null}
          <p id="digital-credential-selection-help" className="flex items-start gap-2 text-sm text-[#52678f]">
            <InfoIcon /> <span>Debes seleccionar una credencial para continuar.</span>
          </p>
        </div>
        <FlowNavigationButton variant="primary" onClick={onSubmit} disabled={!hasValidSelection || submitting}>
          {submitting ? "Continuando…" : "Continuar"}<ArrowIcon />
        </FlowNavigationButton>
      </div>
    </FlowStepContent>
  );
}

function DigitalCredentialRow({ digitalCredential, position, checked, onSelect }: {
  digitalCredential: DigitalCredentialItem; position: number; checked: boolean;
  onSelect: (selection: DigitalCredentialSelection) => void;
}) {
  const visibleName = `Credencial verificable vigente ${String(position).padStart(2, "0")}`;

  return (
    <label
      data-selected={checked ? "true" : "false"}
      className={`grid cursor-pointer grid-cols-[32px_minmax(0,1fr)] items-start gap-x-3 gap-y-2 rounded-xl border px-4 py-3.5 transition-[border-color,background-color] duration-200 focus-within:ring-1 focus-within:ring-inset focus-within:ring-[#0755df] md:grid-cols-[64px_minmax(0,1fr)_160px_96px] md:items-center md:gap-x-4 md:px-5 ${checked ? "border-[#1768f2] bg-[#f5f8ff]" : "border-[#dbe4f1] bg-white hover:border-[#8eafe9] hover:bg-[#fbfdff]"}`}
    >
      <input type="radio" name="selected-digital-credential" value={credentialKey(digitalCredential)}
        checked={checked} onChange={() => onSelect({
          digitalCredentialUuid: digitalCredential.digitalCredentialUuid,
          statusListIndex: digitalCredential.statusListIndex,
        })}
        aria-label={`Seleccionar credencial con índice ${digitalCredential.statusListIndex}`}
        className="sr-only" />

      <span
        aria-hidden="true"
        className={`mt-3 grid size-6 shrink-0 place-items-center justify-self-center rounded-full border-2 transition-colors duration-200 md:mt-0 ${checked ? "border-[#1768f2] bg-[#1768f2] text-white" : "border-[#aab5c8] bg-white text-transparent"}`}
      >
        <CheckIcon />
      </span>

      <span className="min-w-0 md:col-start-2 md:row-start-1">
        <span className="flex min-w-0 items-center gap-3">
          <span
            aria-hidden="true"
            className="grid size-14 shrink-0 place-items-center rounded-lg border border-[#e1e5eb] bg-[#f8f9fb] text-[#314a73]"
          >
            <DigitalCredentialIcon />
          </span>
          <span className="min-w-0">
            <span className="block text-base font-semibold leading-6 text-[#061a50] md:whitespace-nowrap">
              {visibleName}
            </span>
            <span className="mt-1 block text-sm text-[#52678f]">
              Índice de credencial <span className="ml-2 font-medium text-[#173568]">{digitalCredential.statusListIndex}</span>
            </span>
          </span>
        </span>
      </span>

      <span className="col-start-2 text-sm text-[#425b72] md:col-start-3 md:row-start-1">
        <span className="block text-xs font-medium text-[#687893]">Creado el</span>
        <span className="mt-0.5 block font-medium text-[#173568] md:whitespace-nowrap">{formatDate(digitalCredential.emissionCreatedAt)}</span>
      </span>

      <span className="col-start-2 w-fit rounded-full bg-[#e7f5ed] px-3.5 py-1.5 text-xs font-medium text-[#087447] md:col-start-4 md:row-start-1 md:justify-self-center">
        ✓ Vigente
      </span>
    </label>
  );
}

function RevokedDigitalCredentialRow({ digitalCredential, position }: {
  digitalCredential: DigitalCredentialItem;
  position: number;
}) {
  const visibleName = `Credencial verificable revocada ${String(position).padStart(2, "0")}`;

  return (
    <article
      aria-label={`${visibleName}, índice ${digitalCredential.statusListIndex}`}
      className="grid grid-cols-[32px_minmax(0,1fr)] items-start gap-x-3 gap-y-2 rounded-xl border border-[#e1e5eb] bg-[#f8f9fb] px-4 py-3.5 text-[#68717f] md:grid-cols-[64px_minmax(0,1fr)_160px_96px] md:items-center md:gap-x-4 md:px-5"
    >
      <span aria-hidden="true" className="mt-3 grid size-6 place-items-center justify-self-center rounded-full border border-[#c8cfd9] bg-[#eef1f4] text-[#7a8492] md:mt-0">
        <LockIcon />
      </span>

      <span className="min-w-0 md:col-start-2 md:row-start-1">
        <span className="flex min-w-0 items-center gap-3">
          <span aria-hidden="true" className="grid size-14 shrink-0 place-items-center rounded-lg border border-[#dfe3e8] bg-[#f1f3f6] text-[#7b8798] opacity-80">
            <DigitalCredentialIcon />
          </span>
          <span className="min-w-0">
            <span className="block text-base font-semibold leading-6 text-[#4f5d72] md:whitespace-nowrap">
              {visibleName}
            </span>
            <span className="mt-1 block text-sm text-[#727d8c]">
              Índice de credencial <span className="ml-2 font-medium text-[#596779]">{digitalCredential.statusListIndex}</span>
            </span>
          </span>
        </span>
      </span>

      <span className="col-start-2 space-y-1 text-sm md:col-start-3 md:row-start-1">
        <span className="block">
          <span className="block text-[11px] font-medium uppercase tracking-[0.03em] text-[#89919c]">Creada el</span>
          <span className="block font-medium text-[#667180] md:whitespace-nowrap">{formatDate(digitalCredential.emissionCreatedAt)}</span>
        </span>
        <span className="block">
          <span className="block text-[11px] font-medium uppercase tracking-[0.03em] text-[#89919c]">Revocada el</span>
          <span className="block font-medium text-[#596473] md:whitespace-nowrap">{formatDate(digitalCredential.revokedAt)}</span>
        </span>
      </span>

      <span className="col-start-2 w-fit rounded-full bg-[#e9ecef] px-3.5 py-1.5 text-xs font-medium text-[#626c79] md:col-start-4 md:row-start-1 md:justify-self-center">
        Revocada
      </span>
    </article>
  );
}

function LoadingState() {
  return <div className="grid min-h-[440px] place-items-center text-center" aria-live="polite" aria-busy="true"><div><span className="mx-auto block size-10 animate-spin rounded-full border-4 border-[#d6e2f7] border-t-reniec-red motion-reduce:animate-none" /><p className="mt-4 font-semibold text-[#52678f]">Consultando tus credenciales verificables…</p></div></div>;
}

function EmptyState({ onExit }: { onExit: () => void }) {
  return <div className="grid min-h-[390px] place-items-center text-center"><div className="max-w-[540px]"><span className="mx-auto grid size-14 place-items-center rounded-full bg-[#edf3ff] text-[#0755df]"><InfoIcon /></span><p className="mt-5 text-xs font-black uppercase tracking-[0.14em] text-reniec-red">Consulta actualizada</p><h1 id="selection-title" className="mt-3 text-2xl font-black text-[#061a50]">Actualmente no existen credenciales disponibles</h1><p className="mt-3 leading-7 text-[#52678f]">La disponibilidad cambió desde la consulta inicial, por lo que no es posible continuar con esta operación.</p><button type="button" onClick={onExit} className="mt-6 inline-flex min-h-11 cursor-pointer items-center justify-center rounded-lg border border-[#b9c8df] px-6 font-bold text-[#173a78] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df]">Volver al inicio</button></div></div>;
}

function ErrorState({ presentation, onRetry }: { presentation: ErrorPresentation; onRetry: () => void }) {
  const href = presentation.action === "home" ? "/" : undefined;
  return <div className="grid min-h-[390px] place-items-center text-center" role="alert"><div className="max-w-[520px]"><h1 id="selection-title" className="text-2xl font-black text-[#061a50]">{presentation.title}</h1><p className="mt-3 leading-7 text-[#52678f]">{presentation.description}</p>{href ? <a href={href} className="mt-6 inline-flex min-h-11 items-center rounded-lg bg-reniec-red px-6 font-bold text-white">Volver al inicio</a> : <button type="button" onClick={presentation.action === "reload" ? () => window.location.reload() : onRetry} className="mt-6 min-h-11 cursor-pointer rounded-lg bg-reniec-red px-6 font-bold text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df]">{presentation.action === "reload" ? "Recargar lista" : "Reintentar"}</button>}</div></div>;
}

function errorPresentation(error: unknown): ErrorPresentation {
  if (error instanceof HttpClientError) {
    if (error.status === 401 || error.code.startsWith("SESSION_")) return { title: "Tu sesión finalizó", description: "Inicia una nueva operación para continuar.", action: "home" };
    if (error.code === "DIGITAL_CREDENTIAL_SELECTION_CONFLICT") return { title: "La selección fue actualizada", description: "Recarga la lista para consultar su estado actual.", action: "reload" };
    if (error.code === "DIGITAL_CREDENTIAL_LIST_IN_PROGRESS") return { title: "Estamos consultando tus credenciales", description: "La consulta continúa en proceso. Espera un momento e inténtalo nuevamente.", action: "retry" };
    if (error.code === "DIGITAL_CREDENTIAL_LIST_INVALID_RESPONSE") return { title: "No pudimos interpretar la lista", description: "La respuesta recibida no fue válida. Puedes intentarlo nuevamente.", action: "retry" };
    if (error.code === "DIGITAL_CREDENTIAL_LIST_TIMEOUT") return { title: "La consulta tardó demasiado", description: "No se realizaron cambios. Puedes intentarlo nuevamente.", action: "retry" };
    if (error.code === "DIGITAL_CREDENTIAL_LIST_UNAVAILABLE") return { title: "Servicio temporalmente no disponible", description: "No podemos consultar tus credenciales en este momento.", action: "retry" };
  }
  return { title: "No pudimos completar la operación", description: "Revisa tu conexión e inténtalo nuevamente.", action: "retry" };
}

function formatDate(value: string | null) {
  if (!value) return "Fecha no disponible";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Fecha no disponible" : dateFormatter.format(date);
}

function InfoIcon() { return <svg viewBox="0 0 24 24" className="mt-0.5 size-5 shrink-0 fill-none stroke-current stroke-2" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5h.01"/></svg>; }
function DigitalCredentialListIcon() { return <svg viewBox="0 0 24 24" className="size-4 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 3h10l4 4v14H5zM15 3v5h5M8 12h7m-7 4h5" strokeLinecap="round" strokeLinejoin="round"/></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5"/></svg>; }
function BackIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M19 12H5m5-5-5 5 5 5"/></svg>; }
function CheckIcon() { return <svg viewBox="0 0 20 20" className="size-4 fill-none stroke-current stroke-[2.5]" aria-hidden="true"><path d="m4.5 10 3.2 3.2 7.8-7.8" /></svg>; }
function LockIcon() { return <svg viewBox="0 0 20 20" className="size-3.5 fill-none stroke-current stroke-[1.7]" aria-hidden="true"><rect x="4.5" y="8.5" width="11" height="8" rx="2"/><path d="M7 8.5V6a3 3 0 0 1 6 0v2.5"/></svg>; }
function DigitalCredentialIcon() {
  return (
    <svg viewBox="0 0 56 64" className="h-12 w-11 fill-none stroke-current md:h-14 md:w-12" aria-hidden="true">
      <path d="M10 3h25l11 11v34H10z" strokeWidth="1.8" />
      <path d="M35 3v12h11M17 20h20M17 27h20M17 34h12" strokeWidth="1.8" />
      <circle cx="36" cy="42" r="7" fill="#f7f9fd" strokeWidth="1.8" />
      <path d="m32.5 48-1.2 11 4.7-3 4.7 3-1.2-11M33.5 42l1.6 1.6 3.5-3.5" strokeWidth="1.8" />
    </svg>
  );
}
