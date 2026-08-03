import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { ReviewView } from "@/components/revocation-review-transition";
import { ProcessingView, SubmissionUncertainView } from "@/components/revocation-review-views";
import { RevocationStepper } from "@/components/revocation-stepper";
import type { RevocationExecution, RevocationReview } from "@/lib/api/revocation-confirmation";

const review: RevocationReview = {
  requestStatus: "REASON_REGISTERED",
  maskedDni: "******91",
  firstName: "ANA",
  digitalCredential: {
    statusListIndex: 31,
    emissionCreatedAt: "2026-07-15T15:24:00Z",
  },
  reasonCode: "OTHER",
  reasonLabel: "Otro motivo",
  otherReason: "Ya no utilizaré esta credencial.",
  consequences: ["La revocación se ejecutará de forma inmediata en el siguiente paso."],
  consentText: "Confirmo que revisé la credencial seleccionada.",
  consentVersion: "REVOCACION_CREDENCIALES_DIGITALES_V1",
  confirmed: false,
};

const processingOutcome: RevocationExecution = {
  state: "PROCESSING",
  requestStatus: "REVOCATION_SUCCEEDED",
  maskedDni: "******91",
  firstName: "ANA",
  digitalCredential: { statusListIndex: 31, emissionCreatedAt: "2026-07-15T15:24:00Z" },
  reasonLabel: "Otro motivo",
  confirmedAt: "2026-07-31T18:00:00Z",
  completedAt: "2026-07-31T18:00:00Z",
  processing: {
    phase: "PROPAGATING",
    startedAt: "2026-07-31T18:00:00Z",
    readyAt: "2026-07-31T18:01:00Z",
    serverTime: "2026-07-31T18:00:30Z",
  },
  receipt: { code: "RV-2026-000121", status: "PENDING", downloadAvailable: false },
};

describe("revisión y confirmación", () => {
  it("presenta el DNI completo sin la fila de identidad y exige consentimiento", () => {
    const markup = renderToStaticMarkup(
      <ReviewView dni="73905791" review={review} accepted={false} submitting={false}
        onAccepted={() => undefined} onBack={() => undefined} onConfirm={() => undefined} />,
    );

    expect(markup).toContain("PASO 4 DE 5");
    expect(markup).toContain("73905791");
	const dniPosition = markup.indexOf("73905791");
	const namePosition = markup.indexOf("ANA");
	const credentialPosition = markup.indexOf("Credencial");
	expect(markup).toContain("Nombre");
	expect(namePosition).toBeGreaterThan(dniPosition);
	expect(namePosition).toBeLessThan(credentialPosition);
    expect(markup).not.toContain("******91");
    expect(markup).not.toContain("Identidad");
    expect(markup).toContain("Ya no utilizaré esta credencial");
    expect(markup).toContain("Confirmar revocación");
    expect(markup).toContain("disabled");
    expect(markup).toContain("bg-[#fff0f5]");
    expect(markup).toContain("bg-[#fffaf2]");
    expect(markup.match(/sm:w-\[280px\]/g)?.length).toBe(2);
    expect(markup).not.toContain("focus-within:");
    expect(markup).toContain("focus-visible:outline-[#0755df]");
    expect(markup).not.toMatch(/UUID|Identificador|11111111/);
  });

  it("omite la fila Nombre si una revisión histórica no contiene el dato", () => {
	const markup = renderToStaticMarkup(
	  <ReviewView dni="73905791" review={{ ...review, firstName: undefined }} accepted={false} submitting={false}
		onAccepted={() => undefined} onBack={() => undefined} onConfirm={() => undefined} />,
	);

	expect(markup).not.toContain(">Nombre<");
  });

  it("bloquea controles y anuncia el procesamiento", () => {
    const markup = renderToStaticMarkup(
      <ReviewView dni="73905791" review={review} accepted submitting
        onAccepted={() => undefined} onBack={() => undefined} onConfirm={() => undefined} />,
    );

    expect(markup).toContain("Revocando credencial…");
    expect(markup).toContain("La confirmación está en proceso");
    expect(markup.match(/disabled/g)?.length).toBeGreaterThanOrEqual(3);
  });

  it("permite volver solo al paso 3 desde la revisión", () => {
    const markup = renderToStaticMarkup(
      <RevocationStepper currentStep={4} navigableSteps={[3]} onNavigate={() => undefined} />,
    );

    expect(markup).toContain("Paso completado 3: Motivo");
    expect(markup).toContain("Paso actual 4: Confirmación");
    expect(markup).toContain("Paso pendiente 5: Constancia");
    expect(markup.match(/disabled/g)?.length).toBe(4);
  });

  it("mantiene el borrador local y ejecuta una confirmación idempotente", () => {
    const source = readFileSync(resolve(process.cwd(), "components/revocation-review-transition.tsx"), "utf8");
    expect(source).not.toMatch(/localStorage|sessionStorage/i);
    expect(source).toContain("previewCurrentRevocation");
    expect(source).toContain("getConfirmedRevocationReview");
    expect(source).toContain("confirmCurrentRevocation");
    expect(source).toContain("retryCurrentRevocationReceipt");
    expect(source).toContain("submissionInFlight.current");
    expect(source).toContain("uncertainOnTransportFailure");
    expect(source).toContain('error.code === "NETWORK_ERROR"');
    expect(source).toContain('error.code === "TIMEOUT"');
    expect(source).toContain("onConfirm={() => void submit()}");
    expect(source).toContain("getCurrentRevocationOutcome");
    expect(source).toContain("5_000");
    expect(source).not.toMatch(/window\.alert|TEMPORARY_CONFIRMATION_NOTICE/);
  });

  it("muestra progreso autoritativo y tiempo restante durante la propagación", () => {
    const markup = renderToStaticMarkup(<ProcessingView outcome={processingOutcome} />);

    expect(markup).toContain("Completando la revocación");
    expect(markup).toContain("Tiempo estimado restante");
    expect(markup).toContain("00:30");
    expect(markup).toContain('dateTime="PT30S"');
    expect(markup).toContain("30 segundos restantes estimados");
    expect(markup).toContain('role="progressbar"');
    expect(markup).toContain('aria-valuenow="50"');
    expect(markup).toContain("aproximadamente un minuto");
    expect(markup).toContain("propagation-wave");
    expect(markup).not.toContain("Revocación en proceso");
    expect(markup).not.toContain("Propagando de forma segura");
    expect(markup).not.toContain("No cierres sesión");
  });

  it("cambia a progreso indeterminado mientras se genera la constancia", () => {
    const outcome: RevocationExecution = {
      ...processingOutcome,
      processing: {
        ...processingOutcome.processing!,
        phase: "GENERATING",
        serverTime: "2026-07-31T18:01:02Z",
      },
      receipt: { code: "RV-2026-000121", status: "GENERATING", downloadAvailable: false },
    };
    const markup = renderToStaticMarkup(<ProcessingView outcome={outcome} />);

    expect(markup).toContain("Preparando tu constancia");
    expect(markup).toContain("Ya casi terminamos.");
    expect(markup).toContain('aria-label="Preparando la constancia"');
    expect(markup).toContain('role="progressbar"');
    expect(markup).not.toContain("Tiempo estimado restante");
    expect(markup).not.toContain("00:30");
  });

  it("explica la recuperación idempotente cuando se interrumpe la confirmación", () => {
    const markup = renderToStaticMarkup(
      <SubmissionUncertainView submitting={false} onRetry={() => undefined} />,
    );

    expect(markup).toContain("Aún no podemos confirmar el resultado");
    expect(markup).toContain("exactamente la misma operación");
    expect(markup).toContain("Consultar estado");
    expect(markup).not.toContain("Confirmar revocación");
  });
});
