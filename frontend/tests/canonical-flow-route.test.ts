import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { describe, expect, it } from "vitest";

import {
  activeFlowRoute,
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
    expect(activeFlowRoute("local", "production")).toBe(LOCAL_IDENTITY_ROUTE);
    expect(activeFlowRoute("production", "development")).toBe(CANCELLATION_FLOW_ROUTE);
  });

  it.each([
    "app/verificacion-identidad/page.tsx",
    "app/verificacion-identidad/retorno/page.tsx",
  ])("redirige %s hacia la ruta canónica", (file) => {
    const content = source(file);
    expect(content).toContain("redirect(CANCELLATION_FLOW_ROUTE)");
    expect(content).not.toContain("IdentityVerificationPanel");
    expect(content).not.toMatch(/requestId|dni=|step=|code=|token=/i);
  });

  it("mantiene home pública y solo redirige cuando existe una sesión activa", () => {
    const home = source("app/page.tsx");
    expect(home).toContain("readServerFlowSession");
    expect(home).toContain("<PublicCancellationEntry />");
    expect(home).toContain("activeFlowRoute");
    expect(home).toContain("redirect(flowRoute)");
  });

  it("renderiza el paso 1 en /autorizacion solo para el ambiente local", () => {
    const authorizationPage = source("app/autorizacion/page.tsx");
    const cancellationPage = source("app/cancelacion/page.tsx");

    expect(authorizationPage).toContain("if (!usesLocalIdentityRoute()) redirect(CANCELLATION_FLOW_ROUTE)");
    expect(authorizationPage).toContain("<CancellationFlow />");
    expect(cancellationPage).toContain("<CancellationFlow />");
    expect(source("app/cancelacion/layout.tsx")).toContain("requireServerFlowSession");
    expect(source("app/autorizacion/layout.tsx")).toContain("requireServerFlowSession");
    expect(source("app/api/session/refresh/route.ts")).toContain("getSetCookie");
    expect(source("app/api/session/refresh/route.ts")).toContain("cancelacion_refresh");
    expect(source("app/api/session/refresh/route.ts")).not.toContain("cookies.getAll");
    expect(source("lib/server-flow-session.ts")).not.toContain("store.getAll");
    expect(authorizationPage).not.toMatch(/requestId|dni=|step=|code=|token=/i);
  });

  it("consulta el contexto protegido al recargar sin persistencia del navegador", () => {
    const flow = source("components/cancellation-flow.tsx");
    expect(flow).toContain("getCurrentIdentityVerification");
    expect(flow).toContain('setView({ kind: "identity" })');
    expect(source("components/public-cancellation-entry.tsx")).toContain("router.push");
    expect(source("lib/http-client.ts")).toContain("refreshInFlight");
    expect(source("components/internal-flow-header.tsx")).toContain("BroadcastChannel");
    expect(source("components/internal-flow-header.tsx")).toContain("Cerrar sesión");
    expect(flow).not.toMatch(/window\.location|URLSearchParams/);
    expect(flow).not.toMatch(/localStorage|sessionStorage/);
    expect(flow).toContain("const [draft, setDraft]");
    expect(flow).toContain("certificateUuid: null");
    expect(flow).toContain("reasonCode: null");
    expect(flow).toContain("recoverConfirmed={view.confirmed}");
    expect(source("lib/api/flow-session.ts")).not.toContain('nextStep !== "REASON"');
  });
});
