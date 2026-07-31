import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { CancellationReceiptTransition } from "@/components/cancellation-receipt-transition";
import type { CancellationExecution } from "@/lib/api/cancellation-confirmation";

const outcome: CancellationExecution = {
  state: "SUCCEEDED",
  requestStatus: "RECEIPT_AVAILABLE",
  maskedDni: "******91",
  certificate: { orderNumber: "0000123456", emissionCreatedAt: "2026-07-15T15:24:00Z" },
  reasonLabel: "Robo",
  completedAt: "2026-07-30T18:00:00Z",
  receipt: {
    code: "CD-2026-000128",
    status: "AVAILABLE",
    availableAt: "2026-07-30T18:00:01Z",
    downloadAvailable: true,
  },
};

describe("constancia de cancelación", () => {
  it("presenta únicamente el certificado procesado y permite descargar", () => {
    const markup = renderToStaticMarkup(
      <CancellationReceiptTransition dni="73905791" initialData={outcome} />,
    );
    expect(markup).toContain("PASO 5 DE 5");
    expect(markup).toContain("Constancia generada correctamente");
    expect(markup).toContain("73905791");
    expect(markup).not.toContain("******91");
    expect(markup).not.toContain("Identidad");
    expect(markup).toContain("Orden 0000123456");
    expect(markup).toContain("CD-2026-000128");
    expect(markup).toContain("Descargar constancia");
    expect(markup).not.toContain("Finalizar");
    expect(markup).toContain("bg-[#fff0f5]");
    expect(markup).toContain("bg-[#fff7fa]");
    expect(markup).toContain("Esta acción no afecta tu DNI ni tu identidad civil");
    expect(markup).not.toContain("ID Perú");
    expect(markup).toContain("receipt-confirmation-complete-v2.png");
    expect(markup).toContain("únicamente el certificado digital seleccionado");
    expect(markup).not.toMatch(/UUID|11111111/);
  });
});
