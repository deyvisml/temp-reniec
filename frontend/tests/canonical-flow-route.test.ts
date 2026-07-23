import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { describe, expect, it } from "vitest";

import {
  CANCELLATION_FLOW_ROUTE,
  LOCAL_IDENTITY_ROUTE,
  usesLocalIdentityRoute,
} from "@/lib/routes";

function source(path: string) {
  return readFileSync(resolve(process.cwd(), path), "utf8");
}

describe("rutas del flujo ciudadano", () => {
  it("mantiene una entrada canónica y una ruta local para la autenticación", () => {
    expect(CANCELLATION_FLOW_ROUTE).toBe("/cancelacion");
    expect(LOCAL_IDENTITY_ROUTE).toBe("/autorizacion");
    expect(source("lib/routes.ts")).toContain('CANCELLATION_FLOW_ROUTE = "/cancelacion"');
    expect(usesLocalIdentityRoute("local", "production")).toBe(true);
    expect(usesLocalIdentityRoute("production", "development")).toBe(false);
    expect(usesLocalIdentityRoute(undefined, "development")).toBe(true);
    expect(usesLocalIdentityRoute(undefined, "production")).toBe(false);
  });

  it.each([
    "app/page.tsx",
    "app/verificacion-identidad/page.tsx",
    "app/verificacion-identidad/retorno/page.tsx",
  ])("redirige %s hacia la ruta canónica", (file) => {
    const content = source(file);
    expect(content).toContain("redirect(CANCELLATION_FLOW_ROUTE)");
    expect(content).not.toContain("IdentityVerificationPanel");
    expect(content).not.toMatch(/requestId|dni=|step=|code=|token=/i);
  });

  it("renderiza el paso 1 en /autorizacion solo para el ambiente local", () => {
    const authorizationPage = source("app/autorizacion/page.tsx");
    const cancellationPage = source("app/cancelacion/page.tsx");

    expect(authorizationPage).toContain("if (!usesLocalIdentityRoute()) redirect(CANCELLATION_FLOW_ROUTE)");
    expect(authorizationPage).toContain('<CancellationFlow initialRoute="identity" />');
    expect(cancellationPage).toContain("usesLocalIdentityRoute() ? LOCAL_IDENTITY_ROUTE : undefined");
    expect(authorizationPage).not.toMatch(/requestId|dni=|step=|code=|token=/i);
  });

  it("navega al paso local y consulta el contexto temporal al recargar", () => {
    const flow = source("components/cancellation-flow.tsx");
    expect(flow).toContain("getCurrentIdentityVerification");
    expect(flow).toContain('setView({ kind: "identity" })');
    expect(flow).toContain("router.push(identityRoute)");
    expect(flow).not.toMatch(/window\.location|URLSearchParams/);
    expect(flow).not.toMatch(/localStorage|sessionStorage/);
  });
});
