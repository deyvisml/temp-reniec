import { renderToStaticMarkup } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";

import RevocationPage from "@/app/revocacion/page";
import RootLayout, { metadata } from "@/app/layout";
import NotFound from "@/app/not-found";
import { RevocationEntry } from "@/components/revocation-entry";
import { InternalFlowHeaderActions } from "@/components/internal-flow-header";
import { parseCurrentFlowSession } from "@/lib/api/flow-session";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => "/",
  useSearchParams: () => new URLSearchParams(),
}));

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("base application rendering", () => {
  it("renders the real citizen home and accessible DNI form", () => {
    const html = renderToStaticMarkup(<RevocationEntry onContinue={() => undefined} />);

    expect(html).toContain("Revocación de <span>credenciales digitales</span>");
    expect(html).toContain("Ingresa tu DNI para comenzar");
    expect(html).toContain('aria-label="Consulta de credenciales digitales"');
    expect(html).toContain("<form");
    expect(html).toContain('inputMode="numeric"');
    expect(html).toContain('maxLength="8"');
    expect(html).not.toContain("Proyecto en preparación");
    expect(html).not.toContain("Comprobando integración");
    expect(html).not.toContain("ID Perú");
  });

  it("renders the Spanish semantic root shell around its children", () => {
    const html = renderToStaticMarkup(
      <RootLayout>
        <p>Contenido de prueba</p>
      </RootLayout>,
    );

    expect(html).toContain('<html lang="es">');
    expect(html).toContain("<header");
    expect(html).toContain('<main id="main-content" tabindex="-1"');
    expect(html).toContain("<footer");
    expect(html).toContain('href="#main-content"');
    expect(html).toContain('id="global-messages"');
    expect(html).toContain("Contenido de prueba");
    expect(metadata.title).toBe("Revocación de credenciales digitales");
    expect(metadata.description).toContain("Consulta e inicia");
  });

  it("renders the custom not-found page with a home link", () => {
    const html = renderToStaticMarkup(<NotFound />);

    expect(html).toContain("Recurso no encontrado");
    expect(html).toContain('href="/revocacion"');
    expect(html).toContain("Volver al inicio");
  });

  it("renders the canonical flow coordinator", () => {
    const page = renderToStaticMarkup(<RevocationPage />);
    expect(page).toContain("Preparando el trámite");
    expect(page).toContain('aria-busy="true"');
  });

  it("renders the authenticated DNI as a profile and an accessible logout action", () => {
    const html = renderToStaticMarkup(
      <InternalFlowHeaderActions dni="00000001" pending={false} onLogout={() => undefined} />,
    );

    expect(html).toContain("Perfil del ciudadano, DNI 00000001");
    expect(html).toContain("00000001");
    expect(html).toContain("Cerrar sesión");
    expect(html).toContain("<button");
    expect(html).not.toContain("******");
    expect(html).not.toMatch(/access|refresh|token|localStorage|sessionStorage/i);
  });

  it("offers a clear retry after a logout failure", () => {
    const html = renderToStaticMarkup(
      <InternalFlowHeaderActions dni="00000001" pending={false} logoutFailed onLogout={() => undefined} />,
    );

    expect(html).toContain("Reintentar salida");
    expect(html).toContain("No pudimos cerrar la sesión");
    expect(html).toContain('role="alert"');
  });

  it("rejects an outdated session response instead of rendering an undefined DNI", () => {
    expect(parseCurrentFlowSession({
      sessionId: 1,
      requestId: 2,
      maskedDni: "******01",
      sessionStatus: "PENDING_IDENTITY",
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
      nextStep: "IDENTITY_VERIFICATION",
    })).toBeNull();
  });

  it("accepts only a complete current-session contract", () => {
    expect(parseCurrentFlowSession({
      sessionId: 1,
      requestId: 2,
      dni: "00000001",
      sessionStatus: "PENDING_IDENTITY",
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
      nextStep: "IDENTITY_VERIFICATION",
    })).toMatchObject({ dni: "00000001", nextStep: "IDENTITY_VERIFICATION" });
  });
});
