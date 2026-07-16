import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { HttpClientError, requestJson } from "@/lib/http-client";

describe("requestJson", () => {
  beforeEach(() => {
    vi.stubEnv("BACKEND_URL", "http://localhost:8080");
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it("returns JSON and correlation while including future cookie credentials", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      jsonResponse({ available: true }, 200, "correlation-123"),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await requestJson<{ available: boolean }>("/technical/example", {
      headers: { "X-Client": "frontend" },
    });

    expect(result).toEqual({ data: { available: true }, correlationId: "correlation-123" });
    expect(fetchMock).toHaveBeenCalledOnce();

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe("http://localhost:8080/technical/example");
    expect(init?.credentials).toBe("include");
    expect(new Headers(init?.headers).get("Accept")).toBe("application/json");
    expect(new Headers(init?.headers).get("X-Client")).toBe("frontend");
  });

  it("maps the backend error contract", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      jsonResponse(
        {
          code: "VALIDATION_ERROR",
          message: "La solicitud contiene datos inválidos.",
          correlationId: "body-correlation",
        },
        400,
        "header-correlation",
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(requestJson("/technical/example")).rejects.toMatchObject({
      name: "HttpClientError",
      code: "VALIDATION_ERROR",
      message: "La solicitud contiene datos inválidos.",
      status: 400,
      correlationId: "header-correlation",
    });
  });

  it("uses a generic network error without exposing the native message", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockRejectedValue(new Error("socket failure with internal host details"));
    vi.stubGlobal("fetch", fetchMock);

    const request = requestJson("/technical/example");

    await expect(request).rejects.toBeInstanceOf(HttpClientError);
    await expect(request).rejects.toMatchObject({
      code: "NETWORK_ERROR",
      message: "No fue posible conectar con el servicio.",
    });
  });

  it("rejects a successful response that is not valid JSON", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response("not-json", {
        status: 200,
        headers: { "Content-Type": "text/plain", "X-Correlation-ID": "invalid-json-123" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(requestJson("/technical/example")).rejects.toMatchObject({
      code: "INVALID_RESPONSE",
      status: 200,
      correlationId: "invalid-json-123",
    });
  });

  it("uses a generic HTTP error when the error body is not JSON", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response("gateway detail", {
        status: 502,
        headers: { "Content-Type": "text/plain" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(requestJson("/technical/example")).rejects.toMatchObject({
      code: "HTTP_ERROR",
      message: "No fue posible completar la solicitud.",
      status: 502,
    });
  });
});

function jsonResponse(body: unknown, status: number, correlationId?: string): Response {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (correlationId) {
    headers.set("X-Correlation-ID", correlationId);
  }
  return new Response(JSON.stringify(body), { status, headers });
}
