import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { ReviewView } from "@/components/cancellation-review-transition";
import { CancellationStepper } from "@/components/cancellation-stepper";
import type { CancellationReview } from "@/lib/api/cancellation-confirmation";

const review: CancellationReview = {
  requestStatus: "REASON_REGISTERED",
  maskedDni: "******91",
  certificate: {
    orderNumber: "0000123456",
    emissionCreatedAt: "2026-07-15T15:24:00Z",
    maskedUuid: "11111111…1111",
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
  it("presenta solamente datos enmascarados y exige consentimiento", () => {
    const markup = renderToStaticMarkup(
      <ReviewView review={review} accepted={false} submitting={false}
        onAccepted={() => undefined} onBack={() => undefined} onConfirm={() => undefined} />,
    );

    expect(markup).toContain("PASO 4 DE 5");
    expect(markup).toContain("******91");
    expect(markup).toContain("11111111…1111");
    expect(markup).toContain("Ya no utilizaré este certificado");
    expect(markup).toContain("Confirmar cancelación");
    expect(markup).toContain("disabled");
    expect(markup).not.toContain("11111111-1111-4111-8111-111111111111");
  });

  it("bloquea controles y anuncia el procesamiento", () => {
    const markup = renderToStaticMarkup(
      <ReviewView review={review} accepted submitting
        onAccepted={() => undefined} onBack={() => undefined} onConfirm={() => undefined} />,
    );

    expect(markup).toContain("Confirmando…");
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

  it("no persiste el resumen ni invoca revocación o constancia", () => {
    const source = readFileSync(resolve(process.cwd(), "components/cancellation-review-transition.tsx"), "utf8");
    expect(source).not.toMatch(/localStorage|sessionStorage|revocation|receipt|constancia/i);
    expect(source).toContain("getCurrentCancellationReview");
    expect(source).toContain("submissionInFlight.current");
  });
});
