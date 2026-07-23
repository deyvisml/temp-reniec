import { renderToStaticMarkup } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";

import RootLayout, { metadata } from "@/app/layout";
import NotFound from "@/app/not-found";
import CancellationPage from "@/app/cancelacion/page";
import { CancellationEntry } from "@/components/cancellation-entry";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("base application rendering", () => {
  it("renders the real citizen home and accessible DNI form", () => {
    const html = renderToStaticMarkup(<CancellationEntry onContinue={() => undefined} />);

    expect(html).toContain("Cancelación de <span>certificados digitales</span>");
    expect(html).toContain("Ingresa tu DNI para comenzar");
    expect(html).toContain('aria-label="Consulta de certificados digitales"');
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
    expect(metadata.description).toContain("Consulta e inicia");
  });

  it("renders the custom not-found page with a home link", () => {
    const html = renderToStaticMarkup(<NotFound />);

    expect(html).toContain("Recurso no encontrado");
    expect(html).toContain('href="/cancelacion"');
    expect(html).toContain("Volver al inicio");
  });

  it("renders the canonical flow coordinator", () => {
    const page = renderToStaticMarkup(<CancellationPage />);
    expect(page).toContain("Preparando el trámite");
    expect(page).toContain('aria-busy="true"');
  });
});
