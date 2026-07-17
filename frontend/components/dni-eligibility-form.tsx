"use client";

import { useEffect, useRef, useState } from "react";

import { startCancellationRequest } from "@/lib/api/cancellation-requests";
import type { CancellationRequestResponse } from "@/lib/api/contracts";
import { HttpClientError } from "@/lib/http-client";

export const DNI_PATTERN = /^[0-9]{8}$/;

export function validateDni(value: string): string | undefined {
  if (!value) return "Ingresa tu número de DNI.";
  if (!DNI_PATTERN.test(value)) return "El DNI debe contener exactamente 8 dígitos numéricos.";
  return undefined;
}

export function buildIdentityPath(requestId: number): string {
  return `/verificacion-identidad?requestId=${requestId}`;
}

type ViewState =
  | { kind: "form" }
  | { kind: "eligible"; response: CancellationRequestResponse }
  | { kind: "not-eligible"; response: CancellationRequestResponse }
  | { kind: "inconclusive"; response: CancellationRequestResponse }
  | { kind: "error"; title: string; message: string; correlationId?: string };

export function DniEligibilityForm() {
  const [dni, setDni] = useState("");
  const [fieldError, setFieldError] = useState<string>();
  const [pending, setPending] = useState(false);
  const [view, setView] = useState<ViewState>({ kind: "form" });
  const inputRef = useRef<HTMLInputElement>(null);
  const controllerRef = useRef<AbortController | undefined>(undefined);

  useEffect(() => () => controllerRef.current?.abort(), []);

  async function submit() {
    if (pending) return;
    const validation = validateDni(dni);
    if (validation) {
      setFieldError(validation);
      inputRef.current?.focus();
      return;
    }

    setFieldError(undefined);
    setPending(true);
    setView({ kind: "form" });
    const controller = new AbortController();
    controllerRef.current = controller;

    try {
      const result = await startCancellationRequest(dni, controller.signal);
      const response = result.data;
      if (!response) throw new HttpClientError("Respuesta vacía.", { code: "INVALID_RESPONSE" });

      if (response.eligibilityResult === "ELIGIBLE" && response.canContinue) {
        setDni("");
        setView({ kind: "eligible", response });
      } else if (response.eligibilityResult === "NOT_ELIGIBLE") {
        setDni("");
        setView({ kind: "not-eligible", response });
      } else {
        setView({ kind: "inconclusive", response });
      }
    } catch (error) {
      if (error instanceof HttpClientError && error.code === "REQUEST_ABORTED") return;
      setView(errorView(error));
    } finally {
      if (controllerRef.current === controller) controllerRef.current = undefined;
      setPending(false);
    }
  }

  function reset() {
    setDni("");
    setFieldError(undefined);
    setView({ kind: "form" });
    queueMicrotask(() => inputRef.current?.focus());
  }

  if (view.kind !== "form") {
    return <ResultPanel view={view} onRetry={() => void submit()} onReset={reset} />;
  }

  return (
    <form
      className="dni-form"
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        void submit();
      }}
      aria-busy={pending}
    >
      <div className="form-emblem" aria-hidden="true">
        <PersonIcon />
      </div>
      <div className="form-heading">
        <h2>Ingresa tu DNI para comenzar</h2>
        <p>Solo necesitas tu número de DNI para consultar si puedes iniciar la cancelación.</p>
      </div>

      <div className="field-group">
        <label htmlFor="dni">Número de DNI</label>
        <div className={`input-shell${fieldError ? " input-shell-error" : ""}`}>
          <PersonIcon />
          <input
            ref={inputRef}
            id="dni"
            name="dni"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            maxLength={8}
            value={dni}
            aria-invalid={Boolean(fieldError)}
            aria-describedby={fieldError ? "dni-error" : "dni-help"}
            placeholder="Ingresa tu DNI (8 dígitos)"
            disabled={pending}
            onChange={(event) => {
              const value = event.target.value.replace(/[^0-9]/g, "").slice(0, 8);
              setDni(value);
              if (fieldError) setFieldError(validateDni(value));
            }}
          />
        </div>
        <p id={fieldError ? "dni-error" : "dni-help"} className={fieldError ? "field-error" : "field-help"}>
          <InfoIcon />
          {fieldError ?? "Debe contener 8 dígitos numéricos."}
        </p>
      </div>

      <button className="primary-action" type="submit" disabled={pending}>
        <span>{pending ? "Consultando certificados…" : "Iniciar cancelación"}</span>
        {pending ? <span className="spinner" aria-hidden="true" /> : <ArrowIcon />}
      </button>
      <p className="security-note"><ShieldIcon /> Tu información se utiliza únicamente para iniciar esta consulta.</p>
      <span className="sr-only" aria-live="polite">
        {pending ? "Consultando la disponibilidad de certificados digitales." : ""}
      </span>
    </form>
  );
}

function ResultPanel({ view, onRetry, onReset }: {
  view: Exclude<ViewState, { kind: "form" }>;
  onRetry: () => void;
  onReset: () => void;
}) {
  if (view.kind === "eligible") {
    const requestId = view.response.requestId;
    return (
      <section className="result-panel result-success" aria-live="polite" aria-labelledby="result-title">
        <div className="result-icon"><CheckIcon /></div>
        <p className="result-kicker">Consulta completada</p>
        <h2 id="result-title">Puedes continuar con la verificación de identidad</h2>
        <p>Encontramos certificados digitales susceptibles de cancelación para el DNI {view.response.maskedDni}.</p>
        {requestId ? <a className="primary-action" href={buildIdentityPath(requestId)}>Continuar <ArrowIcon /></a> : null}
        <button className="secondary-action" type="button" onClick={onReset}>Realizar otra consulta</button>
      </section>
    );
  }

  if (view.kind === "not-eligible") {
    return (
      <section className="result-panel" aria-live="polite" aria-labelledby="result-title">
        <div className="result-icon"><InfoIcon /></div>
        <p className="result-kicker">Consulta completada</p>
        <h2 id="result-title">No es posible continuar con la cancelación</h2>
        <p>No encontramos certificados digitales susceptibles de cancelación. Esta consulta no afecta tu DNI ni tu identidad.</p>
        <button className="secondary-action" type="button" onClick={onReset}>Volver al inicio</button>
      </section>
    );
  }

  if (view.kind === "inconclusive") {
    return (
      <section className="result-panel result-warning" aria-live="polite" aria-labelledby="result-title">
        <div className="result-icon"><InfoIcon /></div>
        <h2 id="result-title">No pudimos confirmar el resultado</h2>
        <p>La consulta no fue concluyente. Puedes intentarlo nuevamente de forma segura.</p>
        <button className="primary-action" type="button" onClick={onRetry}>Reintentar <ArrowIcon /></button>
        <button className="secondary-action" type="button" onClick={onReset}>Ingresar otro DNI</button>
      </section>
    );
  }

  return (
    <section className="result-panel result-warning" role="alert" aria-labelledby="result-title">
      <div className="result-icon"><InfoIcon /></div>
      <h2 id="result-title">{view.title}</h2>
      <p>{view.message}</p>
      {view.correlationId ? <p className="correlation">Código de atención: {view.correlationId}</p> : null}
      <button className="primary-action" type="button" onClick={onRetry}>Reintentar <ArrowIcon /></button>
      <button className="secondary-action" type="button" onClick={onReset}>Ingresar otro DNI</button>
    </section>
  );
}

function errorView(error: unknown): Extract<ViewState, { kind: "error" }> {
  if (!(error instanceof HttpClientError)) {
    return { kind: "error", title: "No pudimos completar la consulta", message: "Ocurrió un problema inesperado. Inténtalo nuevamente." };
  }
  const messages: Record<string, [string, string]> = {
    TIMEOUT: ["La consulta está tardando demasiado", "Verifica tu conexión e inténtalo nuevamente."],
    ELIGIBILITY_TIMEOUT: ["La consulta está tardando demasiado", "El servicio no respondió a tiempo. Inténtalo nuevamente."],
    NETWORK_ERROR: ["No pudimos conectarnos", "Verifica tu conexión a internet e inténtalo nuevamente."],
    ELIGIBILITY_UNAVAILABLE: ["Servicio temporalmente no disponible", "No podemos consultar los certificados en este momento."],
    ELIGIBILITY_PROVIDER_ERROR: ["No pudimos completar la consulta", "El servicio presentó un inconveniente temporal."],
    ELIGIBILITY_IN_PROGRESS: ["La consulta ya está en proceso", "Espera unos segundos antes de intentarlo nuevamente."],
    CONCURRENT_REQUEST: ["La solicitud fue actualizada", "Inténtalo nuevamente para recuperar su estado actual."],
  };
  const [title, message] = messages[error.code] ?? ["No pudimos completar la consulta", "Inténtalo nuevamente en unos momentos."];
  return { kind: "error", title, message, correlationId: error.correlationId };
}

function PersonIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"/><path d="M5.5 20c.5-4.2 2.7-6.2 6.5-6.2s6 2 6.5 6.2"/></svg>; }
function InfoIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 10.5v6M12 7.5h.01"/></svg>; }
function ShieldIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z"/><path d="m9.3 12 1.8 1.8 3.8-4"/></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 7l5 5-5 5"/></svg>; }
function CheckIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4.3 4.3L19 6.8"/></svg>; }
