"use client";

import { useEffect, useEffectEvent, useRef } from "react";
import type { SweetAlertOptions } from "sweetalert2";

export type IdentityCallbackOutcome =
  | "CANCELLED"
  | "REJECTED"
  | "IDENTITY_MISMATCH"
  | "EXPIRED"
  | "TIMEOUT"
  | "UNAVAILABLE"
  | "ERROR";

const presentations: Record<IdentityCallbackOutcome, { title: string; description: string }> = {
  CANCELLED: {
    title: "Verificación cancelada",
    description: "No se completó la verificación con ID Perú. Puedes intentarlo nuevamente cuando estés listo.",
  },
  REJECTED: {
    title: "No pudimos verificar tu identidad",
    description: "ID Perú no confirmó la verificación. Revisa sus indicaciones e inténtalo nuevamente.",
  },
  IDENTITY_MISMATCH: {
    title: "La identidad no corresponde",
    description: "Por seguridad, no es posible continuar con esta verificación.",
  },
  EXPIRED: {
    title: "La verificación expiró",
    description: "El tiempo disponible terminó. Inicia una nueva verificación para continuar.",
  },
  TIMEOUT: {
    title: "ID Perú tardó demasiado en responder",
    description: "Puedes intentarlo nuevamente en unos momentos.",
  },
  UNAVAILABLE: {
    title: "ID Perú no está disponible",
    description: "No pudimos conectarnos con el servicio. Inténtalo nuevamente más tarde.",
  },
  ERROR: {
    title: "No pudimos completar la verificación",
    description: "Vuelve a intentarlo. Si el problema continúa, inicia nuevamente el trámite.",
  },
};

export function getIdentityCallbackPresentation(outcome: IdentityCallbackOutcome) {
  return presentations[outcome];
}

export function IdentityCallbackAlert({
  outcome,
  onAcknowledge,
}: {
  outcome: IdentityCallbackOutcome;
  onAcknowledge: () => void;
}) {
  const acknowledge = useEffectEvent(onAcknowledge);
  const shownOutcome = useRef<IdentityCallbackOutcome | null>(null);

  useEffect(() => {
    if (shownOutcome.current === outcome) return;
    shownOutcome.current = outcome;
    let active = true;
    let closePopup: (() => void) | undefined;

    void (async () => {
      const { default: Swal } = await import("sweetalert2");
      if (!active) return;
      closePopup = () => Swal.close();
      const presentation = getIdentityCallbackPresentation(outcome);
      await Swal.fire(identityAlertOptions(presentation));
      if (active) acknowledge();
    })();

    return () => {
      active = false;
      closePopup?.();
    };
  }, [outcome]);

  return null;
}

function identityAlertOptions(presentation: { title: string; description: string }): SweetAlertOptions {
  return {
    titleText: presentation.title,
    text: presentation.description,
    icon: "warning",
    iconColor: "#9a5600",
    confirmButtonText: "Aceptar",
    buttonsStyling: false,
    allowOutsideClick: false,
    allowEscapeKey: true,
    focusConfirm: true,
    returnFocus: true,
    width: "min(480px, calc(100% - 2rem))",
    backdrop: "rgba(0, 16, 52, 0.68)",
    customClass: {
      popup: "rounded-2xl! px-6! pb-8! text-[#061a50]!",
      title: "text-balance text-2xl! font-extrabold! text-[#061a50]!",
      htmlContainer: "text-pretty text-[15px]! leading-6! text-[#465a85]!",
      confirmButton: "min-h-12 w-full cursor-pointer rounded-lg border-0 bg-[linear-gradient(100deg,#c3004b,#950037)] px-6 py-3 font-extrabold text-white focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400]",
      actions: "mt-7! w-full! px-2!",
    },
  };
}
