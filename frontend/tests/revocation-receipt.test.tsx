import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { RevocationReceiptTransition } from "@/components/revocation-receipt-transition";
import type { RevocationExecution } from "@/lib/api/revocation-confirmation";

const outcome: RevocationExecution = {
  state: "SUCCEEDED",
  requestStatus: "RECEIPT_AVAILABLE",
  maskedDni: "******91",
  firstName: "ANA",
  digitalCredential: { statusListIndex: 31, emissionCreatedAt: "2026-07-15T15:24:00Z" },
  reasonLabel: "Robo",
  completedAt: "2026-07-30T18:00:00Z",
  receipt: {
    code: "RV-2026-000128",
    status: "AVAILABLE",
    availableAt: "2026-07-30T18:00:01Z",
    downloadAvailable: true,
  },
};

describe("constancia de revocación", () => {
  it("presenta únicamente la credencial procesado y permite descargar", () => {
    const markup = renderToStaticMarkup(
      <RevocationReceiptTransition dni="73905791" initialData={outcome} />,
    );
    expect(markup).toContain("PASO 5 DE 5");
    expect(markup).toContain("Constancia generada correctamente");
    expect(markup).toContain("73905791");
	const dniPosition = markup.indexOf("73905791");
	const namePosition = markup.indexOf("ANA");
	const credentialPosition = markup.indexOf("Credencial");
	expect(markup).toContain("Nombre");
	expect(namePosition).toBeGreaterThan(dniPosition);
	expect(namePosition).toBeLessThan(credentialPosition);
    expect(markup).not.toContain("******91");
    expect(markup).not.toContain("Identidad");
    expect(markup).toContain("Índice 31");
    expect(markup).toContain("RV-2026-000128");
    expect(markup).toContain("Descargar constancia");
    expect(markup).toContain("Guarda esta constancia para tus registros.");
    expect(markup).not.toContain("provisional");
    expect(markup).not.toContain("validación institucional");
    expect(markup).not.toContain("Finalizar");
    expect(markup).toContain("bg-[#fff0f5]");
    expect(markup).toContain("bg-[#fff7fa]");
    expect(markup).toContain("Esta acción no afecta tu DNI ni tu identidad civil");
    expect(markup).not.toContain("ID Perú");
    expect(markup).toContain("receipt-confirmation-complete-v2.png");
    expect(markup).toContain("únicamente la credencial digital seleccionada");
    expect(markup).not.toMatch(/UUID|11111111/);
  });

  it("omite la fila Nombre para una constancia histórica sin ese dato", () => {
	const markup = renderToStaticMarkup(
	  <RevocationReceiptTransition dni="73905791" initialData={{ ...outcome, firstName: undefined }} />,
	);
	expect(markup).not.toContain(">Nombre<");
  });
});
