"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import {
  AvailabilityOutcomeAlert,
  type AvailabilityOutcomeView,
} from "@/components/availability-outcome-alert";
import type { CancellationRequestResponse } from "@/lib/api/contracts";
import { startCancellationRequest } from "@/lib/api/cancellation-requests";
import { HttpClientError } from "@/lib/http-client";
import { RecaptchaCheckbox, RECAPTCHA_SITE_KEY } from "@/components/recaptcha-checkbox";

export const DNI_PATTERN = /^[0-9]{8}$/;

const iconStroke = "fill-none stroke-current stroke-[1.8] [stroke-linecap:round] [stroke-linejoin:round]";
const primaryActionClasses = "flex min-h-[58px] w-full cursor-pointer items-center justify-center gap-3 rounded-lg border-0 bg-[linear-gradient(100deg,#c3004b,#950037)] px-6 font-extrabold text-white no-underline transition-[filter] hover:not-disabled:brightness-95 active:not-disabled:brightness-90 disabled:cursor-not-allowed disabled:opacity-70 focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400] motion-reduce:transition-none max-[480px]:min-h-[55px] [&_svg]:w-[23px]";

export function validateDni(value: string): string | undefined {
  if (!value) return "Ingresa tu número de DNI.";
  if (!DNI_PATTERN.test(value)) return "El DNI debe contener exactamente 8 dígitos numéricos.";
  return undefined;
}

export function canSubmitInitialQuery(
  dni: string,
  recaptchaToken: string,
  pending: boolean,
  siteKeyConfigured = Boolean(RECAPTCHA_SITE_KEY),
): boolean {
  return !pending && !validateDni(dni) && recaptchaToken.length > 0 && siteKeyConfigured;
}

export function isConsistentInitialResponse(response: CancellationRequestResponse): boolean {
  if (!Number.isSafeInteger(response.requestId) || response.requestId <= 0) return false;
  if (!/^\*{6}[0-9]{2}$/.test(response.maskedDni)) return false;

  if (response.availabilityResult === "AVAILABLE") {
    return response.canContinue
      && response.nextStep === "IDENTITY_VERIFICATION"
      && response.requestStatus === "PENDING_IDENTITY_VERIFICATION";
  }

  if (response.availabilityResult === "NOT_AVAILABLE") {
    return !response.canContinue
      && response.nextStep === null
      && response.requestStatus === "NO_CERTIFICATES_AVAILABLE";
  }

  if (response.availabilityResult === "INCONCLUSIVE") {
    return !response.canContinue
      && response.nextStep === null
      && response.requestStatus === "STARTED";
  }

  return false;
}

type ViewState = { kind: "form" } | AvailabilityOutcomeView;

export function DniAvailabilityForm({ onContinue }: { onContinue: () => void }) {
  const [dni, setDni] = useState("");
  const [fieldError, setFieldError] = useState<string>();
  const [pending, setPending] = useState(false);
  const [recaptchaToken, setRecaptchaToken] = useState("");
  const [recaptchaResetKey, setRecaptchaResetKey] = useState(0);
  const [securityMessage, setSecurityMessage] = useState<string>();
  const [view, setView] = useState<ViewState>({ kind: "form" });
  const inputRef = useRef<HTMLInputElement>(null);
  const controllerRef = useRef<AbortController | undefined>(undefined);
  const submissionInFlightRef = useRef(false);

  useEffect(() => () => controllerRef.current?.abort(), []);

  const resetRecaptcha = useCallback((message?: string) => {
    setRecaptchaToken("");
    setRecaptchaResetKey((current) => current + 1);
    setSecurityMessage(message);
  }, []);

  const handleRecaptchaToken = useCallback((token: string) => {
    setRecaptchaToken(token);
    setSecurityMessage(undefined);
  }, []);

  const handleRecaptchaExpired = useCallback(() => {
    resetRecaptcha("La verificación expiró. Marca nuevamente la casilla para continuar.");
  }, [resetRecaptcha]);

  const handleRecaptchaError = useCallback(() => {
    resetRecaptcha("No pudimos cargar la verificación de seguridad. Inténtalo nuevamente más tarde.");
  }, [resetRecaptcha]);

  async function submit() {
    if (submissionInFlightRef.current) return;
    const validation = validateDni(dni);
    if (validation) {
      setFieldError(validation);
      inputRef.current?.focus();
      return;
    }
    if (!recaptchaToken) {
      setSecurityMessage("Completa la verificación de seguridad para continuar.");
      return;
    }

    setFieldError(undefined);
    submissionInFlightRef.current = true;
    setPending(true);
    setView({ kind: "form" });
    const controller = new AbortController();
    controllerRef.current = controller;

    try {
      const result = await startCancellationRequest(dni, recaptchaToken, controller.signal);
      const response = result.data;
      if (!response) throw new HttpClientError("Respuesta vacía.", { code: "INVALID_RESPONSE" });
      if (!isConsistentInitialResponse(response)) {
        throw new HttpClientError("Respuesta inconsistente.", {
          code: "INVALID_RESPONSE",
          correlationId: result.correlationId,
        });
      }

      if (response.availabilityResult === "AVAILABLE") {
        setDni("");
        onContinue();
      } else if (response.availabilityResult === "NOT_AVAILABLE") {
        setDni("");
        setView({ kind: "not-available" });
      } else {
        setView({ kind: "inconclusive" });
      }
    } catch (error) {
      if (error instanceof HttpClientError && error.code === "REQUEST_ABORTED") return;
      const recaptchaMessage = buildRecaptchaErrorMessage(error);
      if (recaptchaMessage) {
        setView({ kind: "form" });
        setSecurityMessage(recaptchaMessage);
      } else {
        setView(buildAvailabilityErrorView(error));
      }
    } finally {
      if (controllerRef.current === controller) controllerRef.current = undefined;
      submissionInFlightRef.current = false;
      setPending(false);
      setRecaptchaToken("");
      setRecaptchaResetKey((current) => current + 1);
    }
  }

  function reset() {
    setDni("");
    setFieldError(undefined);
    resetRecaptcha();
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

      <div className="mb-5" aria-describedby={securityMessage ? "recaptcha-error" : undefined}>
        <RecaptchaCheckbox
          resetKey={recaptchaResetKey}
          disabled={pending}
          onToken={handleRecaptchaToken}
          onExpired={handleRecaptchaExpired}
          onError={handleRecaptchaError}
        />
        {securityMessage ? (
          <p
            id="recaptcha-error"
            className="mx-auto mt-2 text-sm font-semibold text-[#9b003a]"
            role="alert"
            aria-live="polite"
          >
            {securityMessage}
          </p>
        ) : null}
      </div>

      <button className={primaryActionClasses} type="submit" disabled={!canSubmitInitialQuery(dni, recaptchaToken, pending)}>
        <span>{pending ? "Consultando disponibilidad…" : "Iniciar cancelación"}</span>
        {pending ? <span className="size-5 animate-spin rounded-full border-2 border-white/35 border-t-white motion-reduce:animate-none" aria-hidden="true" /> : <ArrowIcon />}
      </button>
      <p className="mt-5 flex items-center justify-center gap-2 border-t border-[#e3e8f1] pt-5 text-xs text-[#607199] max-[480px]:items-start max-[480px]:text-left [&_svg]:w-5 [&_svg]:text-[#1749a8]"><ShieldIcon /> Tu información se utiliza únicamente para iniciar esta consulta.</p>
      <span className="sr-only" aria-live="polite">
        {pending ? "Consultando la disponibilidad de certificados digitales." : ""}
      </span>
      </form>
      {view.kind !== "form" ? (
        <AvailabilityOutcomeAlert
          outcome={view}
          onRetry={() => setView({ kind: "form" })}
          onReset={reset}
        />
      ) : null}
    </>
  );
}

export function buildAvailabilityErrorView(error: unknown): Extract<ViewState, { kind: "error" }> {
  if (!(error instanceof HttpClientError)) {
    return { kind: "error", title: "No pudimos completar la consulta", message: "Ocurrió un problema inesperado. Inténtalo nuevamente." };
  }
  const messages: Record<string, [string, string]> = {
    TIMEOUT: ["La consulta está tardando demasiado", "Verifica tu conexión e inténtalo nuevamente."],
    AVAILABILITY_TIMEOUT: ["La consulta está tardando demasiado", "El servicio no respondió a tiempo. Inténtalo nuevamente."],
    NETWORK_ERROR: ["No pudimos conectarnos", "Verifica tu conexión a internet e inténtalo nuevamente."],
    AVAILABILITY_UNAVAILABLE: ["Servicio temporalmente no disponible", "No podemos consultar los certificados en este momento."],
    AVAILABILITY_PROVIDER_ERROR: ["No pudimos completar la consulta", "El servicio presentó un inconveniente temporal."],
    AVAILABILITY_CHECK_IN_PROGRESS: ["La consulta ya está en proceso", "Espera unos segundos antes de intentarlo nuevamente."],
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

export function buildRecaptchaErrorMessage(error: unknown): string | undefined {
  if (!(error instanceof HttpClientError)) return undefined;
  const messages: Record<string, string> = {
    RECAPTCHA_REQUIRED: "Completa la verificación de seguridad para continuar.",
    RECAPTCHA_REJECTED: "No pudimos validar la verificación. Marca nuevamente la casilla.",
    RECAPTCHA_EXPIRED_OR_DUPLICATE: "La verificación expiró o ya fue utilizada. Complétala nuevamente.",
    RECAPTCHA_UNAVAILABLE: "La verificación de seguridad no está disponible temporalmente.",
    RECAPTCHA_TIMEOUT: "La verificación tardó demasiado. Complétala nuevamente.",
    RECAPTCHA_INVALID_RESPONSE: "No fue posible confirmar la verificación. Complétala nuevamente.",
  };
  return messages[error.code];
}

function PersonIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"/><path d="M5.5 20c.5-4.2 2.7-6.2 6.5-6.2s6 2 6.5 6.2"/></svg>; }
function InfoIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 10.5v6M12 7.5h.01"/></svg>; }
function ShieldIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z"/><path d="m9.3 12 1.8 1.8 3.8-4"/></svg>; }
function ArrowIcon() { return <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 7l5 5-5 5"/></svg>; }
