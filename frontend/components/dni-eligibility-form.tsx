"use client";

import { useEffect, useRef, useState } from "react";

import {
  EligibilityOutcomeAlert,
  type EligibilityOutcome,
} from "@/components/eligibility-outcome-alert";
import { startCancellationRequest } from "@/lib/api/cancellation-requests";
import { HttpClientError } from "@/lib/http-client";

export const DNI_PATTERN = /^[0-9]{8}$/;

const iconStroke = "fill-none stroke-current stroke-[1.8] [stroke-linecap:round] [stroke-linejoin:round]";
const primaryActionClasses = "flex min-h-[58px] w-full cursor-pointer items-center justify-center gap-3 rounded-lg border-0 bg-[linear-gradient(100deg,#c3004b,#950037)] px-6 font-extrabold text-white no-underline shadow-[0_12px_24px_#a8003c27] transition-[transform,box-shadow,filter] hover:not-disabled:-translate-y-0.5 hover:not-disabled:saturate-[1.08] hover:not-disabled:shadow-[0_16px_30px_#a8003c35] disabled:cursor-wait disabled:opacity-70 focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400] motion-reduce:transform-none motion-reduce:transition-none max-[480px]:min-h-[55px] [&_svg]:w-[23px]";

export function validateDni(value: string): string | undefined {
  if (!value) return "Ingresa tu número de DNI.";
  if (!DNI_PATTERN.test(value)) return "El DNI debe contener exactamente 8 dígitos numéricos.";
  return undefined;
}

export function buildIdentityPath(requestId: number): string {
  return `/verificacion-identidad?requestId=${requestId}`;
}

type ViewState = { kind: "form" } | EligibilityOutcome;

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
        setView({
          kind: "eligible",
          continuePath: response.requestId
            ? buildIdentityPath(response.requestId)
            : undefined,
          maskedDni: response.maskedDni,
        });
      } else if (response.eligibilityResult === "NOT_ELIGIBLE") {
        setDni("");
        setView({ kind: "not-eligible" });
      } else {
        setView({ kind: "inconclusive" });
      }
    } catch (error) {
      if (error instanceof HttpClientError && error.code === "REQUEST_ABORTED") return;
      setView(buildEligibilityErrorView(error));
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

  return (
    <>
      <form
        className="text-center"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
        aria-busy={pending}
      >
      <div className="mx-auto mb-3.5 grid size-[54px] place-items-center rounded-full bg-[#e9f1ff] text-[#0755df] [&_svg]:w-[30px]" aria-hidden="true">
        <PersonIcon />
      </div>
      <div>
        <h2 className="m-0 text-[24px] tracking-[-0.02em] text-[#061a50] max-[480px]:text-[21px]">Ingresa tu DNI para comenzar</h2>
        <p className="mt-2 mb-[25px] text-sm text-[#61729a]">Solo necesitas tu número de DNI para consultar si puedes iniciar la cancelación.</p>
      </div>

      <div className="text-left">
        <label className="mb-2 block text-[13px] font-extrabold" htmlFor="dni">Número de DNI</label>
        <div className={`flex h-[58px] items-center gap-3 rounded-lg border-[1.5px] bg-white px-[17px] transition-[border-color,box-shadow] focus-within:border-[#0755df] focus-within:shadow-[0_0_0_4px_#0755df16] max-[480px]:h-[55px] [&>svg]:w-6 [&>svg]:text-[#163e9a] ${fieldError ? "border-[#b00045]" : "border-[#7893ca]"}`}>
          <PersonIcon />
          <input
            className="h-full min-w-0 flex-1 border-0 bg-transparent text-base text-[#0a1c4b] outline-0 placeholder:text-[#7181a5] disabled:cursor-wait"
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
        <p
          id={fieldError ? "dni-error" : "dni-help"}
          className={`mt-[9px] mb-[22px] flex items-center gap-[7px] text-xs [&_svg]:size-4 [&_svg]:shrink-0 ${fieldError ? "font-bold text-[#a0003d]" : "text-[#0755df]"}`}
        >
          <InfoIcon />
          {fieldError ?? "Debe contener 8 dígitos numéricos."}
        </p>
      </div>

      <button className={primaryActionClasses} type="submit" disabled={pending}>
        <span>{pending ? "Consultando certificados…" : "Iniciar cancelación"}</span>
        {pending ? <span className="size-5 animate-spin rounded-full border-2 border-white/35 border-t-white motion-reduce:animate-none" aria-hidden="true" /> : <ArrowIcon />}
      </button>
      <p className="mt-5 flex items-center justify-center gap-2 border-t border-[#e3e8f1] pt-5 text-xs text-[#607199] max-[480px]:items-start max-[480px]:text-left [&_svg]:w-5 [&_svg]:text-[#1749a8]"><ShieldIcon /> Tu información se utiliza únicamente para iniciar esta consulta.</p>
      <span className="sr-only" aria-live="polite">
        {pending ? "Consultando la disponibilidad de certificados digitales." : ""}
      </span>
      </form>
      {view.kind !== "form" ? (
        <EligibilityOutcomeAlert
          outcome={view}
          onContinue={(href) => window.location.assign(href)}
          onRetry={() => void submit()}
          onReset={reset}
        />
      ) : null}
    </>
  );
}

export function buildEligibilityErrorView(error: unknown): Extract<ViewState, { kind: "error" }> {
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
    CONCURRENT_REQUEST: ["No pudimos iniciar la solicitud", "Inténtalo nuevamente en unos momentos."],
    CANCELLATION_REQUEST_IN_PROGRESS: ["No es posible iniciar otra solicitud", "Existe una operación que todavía debe finalizar. Inténtalo nuevamente más adelante."],
  };
  const [title, message] = messages[error.code] ?? ["No pudimos completar la consulta", "Inténtalo nuevamente en unos momentos."];
  return {
    kind: "error",
    title,
    message,
    correlationId: error.correlationId,
    retryable: error.code !== "CANCELLATION_REQUEST_IN_PROGRESS",
  };
}

function PersonIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"/><path d="M5.5 20c.5-4.2 2.7-6.2 6.5-6.2s6 2 6.5 6.2"/></svg>; }
function InfoIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 10.5v6M12 7.5h.01"/></svg>; }
function ShieldIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z"/><path d="m9.3 12 1.8 1.8 3.8-4"/></svg>; }
function ArrowIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 7l5 5-5 5"/></svg>; }
