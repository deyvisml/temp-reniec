import { renderToStaticMarkup } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationStatusView } from "@/components/integration-status";
import { getSystemStatus } from "@/lib/api/system-status";

afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs(); });

describe("technical integration status", () => {
  it("uses the typed real status path", async () => {
    vi.stubEnv("BACKEND_URL", "http://localhost:8080");
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockResolvedValue(new Response(
      JSON.stringify({ status: "UP", database: "UP", timestamp: "2026-07-16T12:00:00Z" }),
      { status: 200, headers: { "Content-Type": "application/json", "X-Correlation-ID": "status-test" } },
    )));
    await expect(getSystemStatus()).resolves.toMatchObject({
      data: { status: "UP", database: "UP" }, correlationId: "status-test",
    });
  });

  it("renders checking text", () => {
    expect(renderToStaticMarkup(<IntegrationStatusView state={{ kind: "checking" }} />)).toContain("Comprobando integración");
  });

  it("renders backend and database availability", () => {
    const html = renderToStaticMarkup(<IntegrationStatusView state={{ kind: "available" }} />);
    expect(html).toContain("Integración disponible");
    expect(html).toContain("Backend y base de datos MySQL");
  });

  it("renders a safe unavailable state with retry and optional correlation", () => {
    const html = renderToStaticMarkup(
      <IntegrationStatusView state={{ kind: "unavailable", correlationId: "safe-reference" }} onRetry={() => {}} />,
    );
    expect(html).toContain("Integración no disponible");
    expect(html).toContain("Reintentar comprobación");
    expect(html).toContain("safe-reference");
    expect(html).not.toContain("stack");
  });
});
