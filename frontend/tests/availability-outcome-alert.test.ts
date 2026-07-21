import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

import {
  getAvailabilityOutcomePresentation,
  getAvailabilitySweetAlertOptions,
  resolveAvailabilityAlertAction,
} from "@/components/availability-outcome-alert";

describe("SweetAlert2 eligibility outcome feedback", () => {
  it("maps an eligible result to an explicit authorized continuation", () => {
    const presentation = getAvailabilityOutcomePresentation({
      kind: "available",
      continuePath: "/verificacion-identidad?requestId=42",
      maskedDni: "******01",
    });

    expect(presentation.tone).toBe("positive");
    expect(presentation.primaryAction).toEqual({
      kind: "continue",
      label: "Continuar con la verificación",
      href: "/verificacion-identidad?requestId=42",
    });
    expect(presentation.secondaryAction?.kind).toBe("reset");
    expect(presentation.description).toContain("******01");
  });

  it("maps a not-eligible result to a calm terminal reset", () => {
    const presentation = getAvailabilityOutcomePresentation({ kind: "not-available" });

    expect(presentation.tone).toBe("informative");
    expect(presentation.title).toBe("No encontramos certificados para cancelar");
    expect(presentation.description).toBe(
      "No encontramos certificados digitales disponibles para cancelar con el DNI ingresado.",
    );
    expect(presentation.primaryAction).toEqual({
      kind: "reset",
      label: "Aceptar",
    });
    expect(presentation.secondaryAction).toBeUndefined();
  });

  it("keeps retries and controlled correlation data explicit", () => {
    const inconclusive = getAvailabilityOutcomePresentation({ kind: "inconclusive" });
    const error = getAvailabilityOutcomePresentation({
      kind: "error",
      title: "Servicio temporalmente no disponible",
      message: "No podemos consultar los certificados en este momento.",
      correlationId: "correlation-test",
    });

    expect(inconclusive.primaryAction.kind).toBe("retry");
    expect(inconclusive.primaryAction.label).toBe("Intentar nuevamente");
    expect(inconclusive.secondaryAction).toBeUndefined();
    expect(error.primaryAction.kind).toBe("retry");
    expect(error.secondaryAction).toBeUndefined();
    expect(error.correlationId).toBe("correlation-test");
  });

  it("acknowledges a protected operation without offering retry or historical recovery", () => {
    const presentation = getAvailabilityOutcomePresentation({
      kind: "error",
      title: "No es posible iniciar otra solicitud",
      message: "Existe una operación que todavía debe finalizar.",
      correlationId: "protected-correlation",
      retryable: false,
    });

    expect(presentation.primaryAction).toEqual({ kind: "reset", label: "Aceptar" });
    expect(presentation.secondaryAction).toBeUndefined();
    expect(presentation.description).not.toMatch(/recuper|constancia|certificado seleccionado/i);
  });

  it("builds supported SweetAlert2 options without dynamic HTML", () => {
    const presentation = getAvailabilityOutcomePresentation({
      kind: "available",
      continuePath: "/verificacion-identidad?requestId=42",
      maskedDni: "<img src=x onerror=alert(1)>",
    });
    const options = getAvailabilitySweetAlertOptions(presentation, false);

    expect(options.titleText).toBe(presentation.title);
    expect(options.text).toContain("<img src=x onerror=alert(1)>");
    expect(options).not.toHaveProperty("html");
    expect(options).not.toHaveProperty("title");
    expect(options.allowOutsideClick).toBe(false);
    expect(options.allowEscapeKey).toBe(true);
    expect(options.buttonsStyling).toBe(false);
    expect(options.showCancelButton).toBe(true);
    expect(options.customClass?.confirmButton).toContain("min-h-12");
    expect(options.customClass?.popup).toContain("max-h-[calc(100dvh-2rem)]");
  });

  it("disables non-essential animation for reduced motion", () => {
    const presentation = getAvailabilityOutcomePresentation({ kind: "not-available" });

    expect(getAvailabilitySweetAlertOptions(presentation, true).animation).toBe(false);
    expect(getAvailabilitySweetAlertOptions(presentation, false).animation).toBe(true);
  });

  it("resolves only explicit or safe dismiss actions", () => {
    const eligible = getAvailabilityOutcomePresentation({
      kind: "available",
      continuePath: "/verificacion-identidad?requestId=42",
    });
    const inconclusive = getAvailabilityOutcomePresentation({ kind: "inconclusive" });

    expect(
      resolveAvailabilityAlertAction(eligible, {
        isConfirmed: true,
        dismiss: undefined,
      })?.kind,
    ).toBe("continue");
    expect(
      resolveAvailabilityAlertAction(eligible, {
        isConfirmed: false,
        dismiss: "cancel",
      })?.kind,
    ).toBe("reset");
    expect(
      resolveAvailabilityAlertAction(inconclusive, {
        isConfirmed: true,
        dismiss: undefined,
      })?.kind,
    ).toBe("retry");
    expect(
      resolveAvailabilityAlertAction(inconclusive, {
        isConfirmed: false,
        dismiss: "esc",
      })?.kind,
    ).toBe("retry");
    expect(
      resolveAvailabilityAlertAction(inconclusive, {
        isConfirmed: false,
        dismiss: "backdrop",
      }),
    ).toBeUndefined();
  });

  it("loads SweetAlert2 on demand and removes the project-owned modal", () => {
    const alertSource = readFileSync(
      join(process.cwd(), "components", "availability-outcome-alert.tsx"),
      "utf8",
    );
    const formSource = readFileSync(
      join(process.cwd(), "components", "dni-availability-form.tsx"),
      "utf8",
    );

    expect(alertSource).toContain('await import("sweetalert2")');
    expect(alertSource).toContain("if (!active) return");
    expect(alertSource).not.toContain("<dialog");
    expect(alertSource).not.toContain("sweetalert2-react-content");
    expect(formSource).toContain("AvailabilityOutcomeAlert");
    expect(formSource).not.toContain("EligibilityOutcomeDialog");
    expect(formSource).not.toContain("ResultPanel");
  });
});
