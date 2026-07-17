import { renderToStaticMarkup } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";

import RootLayout, { metadata } from "@/app/layout";
import NotFound from "@/app/not-found";
import HomePage from "@/app/page";

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("base application rendering", () => {
  it("renders the temporary home page without citizen-flow controls", () => {
    vi.stubEnv("NEXT_PUBLIC_APP_ENV", "test");

    const html = renderToStaticMarkup(<HomePage />);

    expect(html).toContain("Cancelación de certificados digitales");
    expect(html).toContain("Proyecto en preparación");
    expect(html).toContain("Estado técnico");
    expect(html).toContain("Comprobando integración");
    expect(html).toContain(">test<");
    expect(html).not.toContain("<form");
    expect(html).not.toContain("<input");
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
    expect(metadata.description).toContain("Base técnica");
  });

  it("renders the custom not-found page with a home link", () => {
    const html = renderToStaticMarkup(<NotFound />);

    expect(html).toContain("Recurso no encontrado");
    expect(html).toContain('href="/"');
    expect(html).toContain("Volver al inicio");
  });
});
