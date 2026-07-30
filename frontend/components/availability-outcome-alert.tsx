"use client";

import { useEffect, useEffectEvent } from "react";
import type {
  SweetAlertIcon,
  SweetAlertOptions,
  SweetAlertResult,
} from "sweetalert2";

const primaryActionClasses =
  "min-h-12 w-full cursor-pointer rounded-lg border-0 bg-[linear-gradient(100deg,#c3004b,#950037)] px-5 py-3 font-extrabold text-white shadow-[0_6px_8px_#a8003c2b] transition-[transform,box-shadow,filter] hover:-translate-y-0.5 hover:saturate-[1.08] hover:shadow-[0_8px_8px_#a8003c36] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#f4b400] motion-reduce:transform-none motion-reduce:transition-none";

const secondaryActionClasses =
  "min-h-12 w-full cursor-pointer rounded-lg border border-[#b8c6df] bg-white px-5 py-3 font-extrabold text-[#164aa8] transition-colors hover:bg-[#f3f7fd] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#f4b400] motion-reduce:transition-none";

export type AvailabilityOutcomeView =
  | { kind: "not-available" }
  | { kind: "inconclusive" }
  | {
      kind: "error";
      title: string;
      message: string;
      correlationId?: string;
      retryable?: boolean;
    };

export type AvailabilityOutcomeAction = {
  kind: "retry" | "reset";
  label: string;
};

export type AvailabilityOutcomePresentation = {
  tone: "informative" | "warning";
  title: string;
  description: string;
  correlationId?: string;
  primaryAction: AvailabilityOutcomeAction;
  secondaryAction?: AvailabilityOutcomeAction;
};

export function getAvailabilityOutcomePresentation(
  outcome: AvailabilityOutcomeView,
): AvailabilityOutcomePresentation {
  switch (outcome.kind) {
    case "not-available":
      return {
        tone: "informative",
        title: "No encontramos certificados para cancelar",
        description:
          "No encontramos certificados digitales disponibles para cancelar con el DNI ingresado.",
        primaryAction: { kind: "reset", label: "Aceptar" },
      };

    case "inconclusive":
      return {
        tone: "warning",
        title: "No pudimos confirmar el resultado",
        description:
          "La consulta no fue concluyente. Puedes intentarlo nuevamente de forma segura.",
        primaryAction: { kind: "retry", label: "Intentar nuevamente" },
      };

    case "error":
      return {
        tone: "warning",
        title: outcome.title,
        description: outcome.message,
        correlationId: outcome.correlationId,
        primaryAction: outcome.retryable === false
          ? { kind: "reset", label: "Aceptar" }
          : { kind: "retry", label: "Intentar nuevamente" },
      };

    default:
      return assertNever(outcome);
  }
}

export function getAvailabilitySweetAlertOptions(
  presentation: AvailabilityOutcomePresentation,
  reducedMotion: boolean,
): SweetAlertOptions {
  return {
    titleText: presentation.title,
    text: presentation.description,
    icon: toneIcons[presentation.tone],
    iconColor: toneIconColors[presentation.tone],
    width: "min(480px, calc(100% - 2rem))",
    padding: "0 0 2rem",
    color: "#061a50",
    background: "#ffffff",
    backdrop: "rgba(0, 16, 52, 0.68)",
    heightAuto: false,
    allowOutsideClick: false,
    allowEscapeKey: true,
    stopKeydownPropagation: true,
    keydownListenerCapture: true,
    showCloseButton: false,
    showConfirmButton: true,
    showCancelButton: Boolean(presentation.secondaryAction),
    confirmButtonText: presentation.primaryAction.label,
    cancelButtonText: presentation.secondaryAction?.label,
    buttonsStyling: false,
    focusConfirm: true,
    returnFocus: false,
    animation: !reducedMotion,
    customClass: {
      popup:
        "max-h-[calc(100dvh-2rem)]! overflow-y-auto! rounded-2xl! text-[#061a50]! shadow-[0_18px_48px_rgba(0,20,60,0.28)]!",
      icon: "mt-8! mb-5! max-[480px]:mt-6!",
      title:
        "mx-7! p-0! text-balance text-2xl! leading-tight! font-extrabold! tracking-[-0.02em]! text-[#061a50]! max-[480px]:mx-5! max-[480px]:text-[21px]!",
      htmlContainer:
        "mx-7! mt-3! mb-0! text-pretty text-[15px]! leading-6! text-[#465a85]! max-[480px]:mx-5!",
      footer:
        "mx-7! mt-4! mb-0! rounded-md! border-0! bg-[#f2f5fa]! px-3! py-2! font-mono! text-[11px]! leading-4! text-[#465a85]! max-[480px]:mx-5!",
      actions:
        "mx-7! mt-7! mb-0! flex! w-[calc(100%-3.5rem)]! flex-col! gap-3! p-0! max-[480px]:mx-5! max-[480px]:w-[calc(100%-2.5rem)]!",
      confirmButton: primaryActionClasses,
      cancelButton: secondaryActionClasses,
    },
  };
}

export function resolveAvailabilityAlertAction(
  presentation: AvailabilityOutcomePresentation,
  result: Pick<SweetAlertResult, "isConfirmed" | "dismiss">,
): AvailabilityOutcomeAction | undefined {
  if (result.isConfirmed) return presentation.primaryAction;
  if (result.dismiss === "cancel") return presentation.secondaryAction;
  if (result.dismiss === "esc") return safeDismissAction(presentation);
  return undefined;
}

export function AvailabilityOutcomeAlert({
  outcome,
  onRetry,
  onReset,
}: {
  outcome: AvailabilityOutcomeView;
  onRetry: () => void;
  onReset: () => void;
}) {
  const retryEvent = useEffectEvent(onRetry);
  const resetEvent = useEffectEvent(onReset);

  useEffect(() => {
    let active = true;
    let closeActivePopup: (() => void) | undefined;

    void (async () => {
      const { default: Swal } = await import("sweetalert2");
      if (!active) return;

      closeActivePopup = () => Swal.close();
      const presentation = getAvailabilityOutcomePresentation(outcome);
      const reducedMotion = window.matchMedia(
        "(prefers-reduced-motion: reduce)",
      ).matches;
      const footer = createCorrelationFooter(presentation.correlationId);
      const result = await Swal.fire({
        ...getAvailabilitySweetAlertOptions(presentation, reducedMotion),
        footer,
      });

      if (!active) return;
      const action = resolveAvailabilityAlertAction(presentation, result);
      if (!action) return;

      if (action.kind === "retry") retryEvent();
      if (action.kind === "reset") resetEvent();
    })();

    return () => {
      active = false;
      closeActivePopup?.();
    };
  }, [outcome]);

  return null;
}

const toneIcons = {
  informative: "info",
  warning: "warning",
} satisfies Record<AvailabilityOutcomePresentation["tone"], SweetAlertIcon>;

const toneIconColors = {
  informative: "#1749a8",
  warning: "#9a5600",
} satisfies Record<AvailabilityOutcomePresentation["tone"], string>;

function safeDismissAction(
  presentation: AvailabilityOutcomePresentation,
): AvailabilityOutcomeAction | undefined {
  if (
    presentation.primaryAction.kind === "reset"
    || presentation.primaryAction.kind === "retry"
  ) {
    return presentation.primaryAction;
  }
  if (presentation.secondaryAction?.kind === "reset") {
    return presentation.secondaryAction;
  }
  return undefined;
}

function createCorrelationFooter(correlationId?: string): HTMLElement | undefined {
  if (!correlationId) return undefined;
  const footer = document.createElement("span");
  footer.textContent = `Código de atención: ${correlationId}`;
  return footer;
}

function assertNever(value: never): never {
  throw new Error(`Resultado de disponibilidad no soportado: ${String(value)}`);
}
