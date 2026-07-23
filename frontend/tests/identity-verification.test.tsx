import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

import { CertificateSelectionTransition } from "@/components/certificate-selection-transition";
import {
  getIdentityCallbackPresentation,
  type IdentityCallbackOutcome,
} from "@/components/identity-callback-alert";
import { IdentityVerificationPanel } from "@/components/identity-verification-panel";
import { CancellationFlow, asIdentityCallbackOutcome } from "@/components/cancellation-flow";

describe("paso de verificación de identidad", () => {
  it("renderiza el paso uno de cinco con explicación y acciones accesibles", () => {
    const markup = renderToStaticMarkup(<IdentityVerificationPanel />);

    expect(markup).toContain("PASO 1 DE 5");
    expect(markup).toContain("Verifica tu identidad");
    expect(markup).toContain("Iniciar verificación facial con ID Perú");
    expect(markup).toContain("Seguro");
    expect(markup).toContain("Rápido");
    expect(markup).toContain("Oficial");
    expect(markup).toContain("%2Fimages%2Fperson.png");
    expect(markup).toContain('aria-label="Progreso del proceso"');
    expect(markup).toContain('aria-current="step"');
    expect(markup).toContain("Paso actual 1: Autenticación");
    expect(markup).toContain("Paso pendiente 2: Selección");
    expect(markup).not.toContain("Regresar");
    expect(markup).not.toContain("requestId");
  });

  it("muestra un estado de procesamiento mientras resuelve el contexto vigente", () => {
    const markup = renderToStaticMarkup(<CancellationFlow />);

    expect(markup).toContain("Preparando el trámite");
    expect(markup).toContain('aria-busy="true"');
  });

  it("normaliza únicamente resultados de callback permitidos", () => {
    expect(asIdentityCallbackOutcome("CANCELLED")).toBe("CANCELLED");
    expect(asIdentityCallbackOutcome("IDENTITY_MISMATCH")).toBe("IDENTITY_MISMATCH");
    expect(asIdentityCallbackOutcome("VERIFIED")).toBeUndefined();
    expect(asIdentityCallbackOutcome("forced-step-2")).toBeUndefined();
  });

  it.each<IdentityCallbackOutcome>([
    "CANCELLED",
    "REJECTED",
    "IDENTITY_MISMATCH",
    "EXPIRED",
    "TIMEOUT",
    "UNAVAILABLE",
    "ERROR",
  ])("define un aviso ciudadano para %s", (outcome) => {
    const presentation = getIdentityCallbackPresentation(outcome);
    expect(presentation.title.length).toBeGreaterThan(5);
    expect(presentation.description.length).toBeGreaterThan(10);
    expect(JSON.stringify(presentation)).not.toMatch(/state|code|token|session_state/i);
  });

  it("presenta la transición mínima al paso 2 sin inventar certificados", () => {
    const markup = renderToStaticMarkup(<CertificateSelectionTransition />);

    expect(markup).toContain("PASO 2 DE 5");
    expect(markup).toContain("Selección de certificados");
    expect(markup).toContain("Tu identidad fue verificada correctamente");
    expect(markup).toContain("Paso actual 2: Selección");
    expect(markup).not.toMatch(/UUID|número de orden|seleccionar certificado/i);
  });

  it("bloquea el doble inicio y usa el aviso compartido para errores", () => {
    const source = readFileSync(resolve(process.cwd(), "components/identity-verification-panel.tsx"), "utf8");
    const alertSource = readFileSync(resolve(process.cwd(), "components/identity-callback-alert.tsx"), "utf8");

    expect(source).toContain("const inFlight = useRef(false)");
    expect(source).toContain("if (inFlight.current) return");
    expect(source).toContain('disabled={view === "starting"}');
    expect(source).toContain("IdentityCallbackAlert");
    expect(source).not.toContain("Verificación completada");
    expect(source).not.toContain("function Stepper");
    expect(alertSource).toContain("const shownOutcome = useRef");
    expect(alertSource).toContain("if (shownOutcome.current === outcome) return");
    expect(alertSource).toContain('confirmButtonText: "Aceptar"');
    expect(alertSource).not.toMatch(/window\.location|router\.(push|replace)/);
  });
});
