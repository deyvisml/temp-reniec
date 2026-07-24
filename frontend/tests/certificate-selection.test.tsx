import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { CertificateSelectionView } from "@/components/certificate-selection-transition";
import { CancellationStepper } from "@/components/cancellation-stepper";
import type { CertificateItem } from "@/lib/api/certificate-listing";

const certificates: CertificateItem[] = [
  {
    orderNumber: "ORD-0001",
    emissionCreatedAt: "2026-07-20T10:00:00Z",
    certificateUuid: "10000000-0000-4000-8000-000000000001",
    availabilityStatus: "AVAILABLE",
    selected: false,
  },
  {
    orderNumber: "ORD-0002",
    emissionCreatedAt: "2026-07-21T11:30:00Z",
    certificateUuid: "10000000-0000-4000-8000-000000000002",
    availabilityStatus: "AVAILABLE",
    selected: false,
  },
];

describe("selección de certificados", () => {
  it("renderiza uno o varios certificados con controles accesibles", () => {
    const markup = renderToStaticMarkup(
      <CertificateSelectionView certificates={certificates} selected={new Set()}
        submitting={false} onToggle={() => undefined} onToggleAll={() => undefined}
        onSubmit={() => undefined} onBack={() => undefined} />,
    );

    expect(markup).toContain("PASO 2 DE 5");
    expect(markup).toContain("ORD-0001");
    expect(markup).toContain("ORD-0002");
    expect(markup).toContain("Seleccionar todos los certificados");
    expect(markup).toContain("Seleccionar certificado ORD-0001");
    expect(markup).toContain("Regresar");
    expect(markup).not.toContain("Solo se cancelarán los certificados seleccionados");
    expect(markup.match(/sm:w-\[280px\]/g)?.length).toBe(2);
    expect(markup).toContain("disabled");
  });

  it("muestra la cantidad exacta y habilita continuar con una selección", () => {
    const markup = renderToStaticMarkup(
      <CertificateSelectionView certificates={certificates}
        selected={new Set([certificates[0].certificateUuid])} submitting={false}
        onToggle={() => undefined} onToggleAll={() => undefined} onSubmit={() => undefined}
        onBack={() => undefined} />,
    );

    expect(markup).toContain("1 seleccionado");
    expect(markup).toContain("checked");
    expect(markup).not.toContain('disabled=""');
  });

  it("mantiene la selección explícita con un único certificado", () => {
    const markup = renderToStaticMarkup(
      <CertificateSelectionView certificates={[certificates[0]]} selected={new Set()}
        submitting={false} onToggle={() => undefined} onToggleAll={() => undefined}
        onSubmit={() => undefined} onBack={() => undefined} />,
    );

    expect(markup).toContain("Certificados vigentes");
    expect(markup).toContain("ORD-0001");
    expect(markup).toContain("0 seleccionados");
    expect(markup).toContain("disabled");
  });

  it("bloquea un segundo envío mientras guarda la selección", () => {
    const markup = renderToStaticMarkup(
      <CertificateSelectionView certificates={certificates}
        selected={new Set(certificates.map(item => item.certificateUuid))} submitting
        onToggle={() => undefined} onToggleAll={() => undefined} onSubmit={() => undefined}
        onBack={() => undefined} />,
    );

    expect(markup).toContain("2 seleccionados");
    expect(markup).toContain("Guardando selección…");
    expect(markup).toContain("disabled");
  });

  it("diferencia el paso completado, actual y pendiente sin permitir volver a autenticación", () => {
    const markup = renderToStaticMarkup(
      <CancellationStepper currentStep={2} />,
    );

    expect(markup).toContain("Paso completado 1: Autenticación");
    expect(markup).toContain("Paso actual 2: Selección");
    expect(markup).toContain("Paso pendiente 3: Motivo");
    expect(markup).toContain('aria-current="step"');
    expect(markup.match(/disabled/g)?.length).toBe(5);
  });

  it("permite volver del paso 3 solamente al paso 2", () => {
    const markup = renderToStaticMarkup(
      <CancellationStepper currentStep={3} navigableSteps={[2]} onNavigate={() => undefined} />,
    );

    expect(markup).toContain("Paso completado 1: Autenticación");
    expect(markup).toContain("Paso completado 2: Selección");
    expect(markup).toContain("Paso actual 3: Motivo");
    expect(markup).toContain("Paso pendiente 4: Confirmación");
    expect(markup.match(/disabled/g)?.length).toBe(4);
  });

  it("no persiste UUID ni selección en almacenamiento o URL del navegador", () => {
    const source = readFileSync(resolve(process.cwd(), "components/certificate-selection-transition.tsx"), "utf8");
    expect(source).not.toMatch(/localStorage|sessionStorage|URLSearchParams/);
    expect(source).toContain("submissionInFlight.current");
    expect(source).toContain("disabled={selected.size === 0 || submitting}");
    expect(source).toContain("CERTIFICATE_LIST_TIMEOUT");
    expect(source).toContain("CERTIFICATE_LIST_UNAVAILABLE");
    expect(source).toContain("CERTIFICATE_LIST_INVALID_RESPONSE");
    expect(source).toContain("CERTIFICATE_SELECTION_CONFLICT");
  });
});
