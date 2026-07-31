import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { ReviewView } from "@/components/cancellation-review-transition";
import { SubmissionUncertainView } from "@/components/cancellation-review-views";
import { CancellationStepper } from "@/components/cancellation-stepper";
import type { CancellationReview } from "@/lib/api/cancellation-confirmation";

const review: CancellationReview = {
  requestStatus: "REASON_REGISTERED",
  maskedDni: "******91",
  certificate: {
    orderNumber: "0000123456",
    emissionCreatedAt: "2026-07-15T15:24:00Z",
  },
  reasonCode: "OTHER",
  reasonLabel: "Otro motivo",
  otherReason: "Ya no utilizaré este certificado.",
  consequences: ["La cancelación se ejecutará de forma inmediata en el siguiente paso."],
  consentText: "Confirmo que revisé el certificado seleccionado.",
  consentVersion: "CANCELACION_CERTIFICADOS_V1",
  confirmed: false,
};

describe("revisión y confirmación", () => {
  it("presenta el DNI completo sin la fila de identidad y exige consentimiento", () => {
    const markup = renderToStaticMarkup(
      <ReviewView dni="73905791" review={review} accepted={false} submitting={false}
        onAccepted={() => undefined} onBack={() => undefined} onConfirm={() => undefined} />,
    );

    expect(markup).toContain("PASO 4 DE 5");
    expect(markup).toContain("73905791");
    expect(markup).not.toContain("******91");
    expect(markup).not.toContain("Identidad");
    expect(markup).toContain("Ya no utilizaré este certificado");
    expect(markup).toContain("Confirmar cancelación");
    expect(markup).toContain("disabled");
    expect(markup).toContain("bg-[#fff0f5]");
    expect(markup).toContain("bg-[#fffaf2]");
    expect(markup.match(/sm:w-\[280px\]/g)?.length).toBe(2);
    expect(markup).not.toContain("focus-within:");
    expect(markup).toContain("focus-visible:outline-[#0755df]");
    expect(markup).not.toMatch(/UUID|Identificador|11111111/);
  });

  it("bloquea controles y anuncia el procesamiento", () => {
    const markup = renderToStaticMarkup(
      <ReviewView dni="73905791" review={review} accepted submitting
        onAccepted={() => undefined} onBack={() => undefined} onConfirm={() => undefined} />,
    );

    expect(markup).toContain("Cancelando certificado…");
    expect(markup).toContain("La confirmación está en proceso");
    expect(markup.match(/disabled/g)?.length).toBeGreaterThanOrEqual(3);
  });

  it("permite volver solo al paso 3 desde la revisión", () => {
    const markup = renderToStaticMarkup(
      <CancellationStepper currentStep={4} navigableSteps={[3]} onNavigate={() => undefined} />,
    );

    expect(markup).toContain("Paso completado 3: Motivo");
    expect(markup).toContain("Paso actual 4: Confirmación");
    expect(markup).toContain("Paso pendiente 5: Constancia");
    expect(markup.match(/disabled/g)?.length).toBe(4);
  });

  it("mantiene el borrador local y ejecuta la operación solo al confirmar", () => {
    const source = readFileSync(resolve(process.cwd(), "components/cancellation-review-transition.tsx"), "utf8");
    expect(source).not.toMatch(/localStorage|sessionStorage/i);
    expect(source).toContain("previewCurrentCancellation");
    expect(source).toContain("getConfirmedCancellationReview");
    expect(source).toContain("confirmCurrentCancellation(complete");
    expect(source).toContain("retryCurrentCancellationReceipt");
    expect(source).toContain("submissionInFlight.current");
    expect(source).toContain("uncertainOnTransportFailure");
    expect(source).toContain('error.code === "NETWORK_ERROR"');
    expect(source).toContain('error.code === "TIMEOUT"');
  });

  it("explica la recuperación idempotente cuando se interrumpe la confirmación", () => {
    const markup = renderToStaticMarkup(
      <SubmissionUncertainView submitting={false} onRetry={() => undefined} />,
    );

    expect(markup).toContain("Aún no podemos confirmar el resultado");
    expect(markup).toContain("exactamente la misma operación");
    expect(markup).toContain("Consultar estado");
    expect(markup).not.toContain("Confirmar cancelación");
  });
});
