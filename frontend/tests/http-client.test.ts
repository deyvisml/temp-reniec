import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { CORRELATION_HEADER, HttpClientError, requestJson, resolveBackendUrl } from "@/lib/http-client";
import { getCurrentFlowSession } from "@/lib/api/flow-session";

describe("requestJson", () => {
  beforeEach(() => {
    vi.stubEnv("BACKEND_URL", "http://server-backend:8080");
    vi.stubEnv("NEXT_PUBLIC_BACKEND_URL", "http://browser-backend:8080");
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it("resolves explicit server and browser URLs", () => {
    expect(resolveBackendUrl("server")).toBe("http://server-backend:8080");
    expect(resolveBackendUrl("browser")).toBe("http://browser-backend:8080");
  });

  it("returns JSON, sends correlation and includes future cookie credentials", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ available: true }, 200, "backend-correlation"));
    vi.stubGlobal("fetch", fetchMock);
    const result = await requestJson<{ available: boolean }>("/technical/example", { headers: { "X-Client": "frontend" } });

    expect(result).toEqual({ data: { available: true }, correlationId: "backend-correlation" });
    const [url, init] = fetchMock.mock.calls[0];
    const headers = new Headers(init?.headers);
    expect(String(url)).toBe("http://server-backend:8080/technical/example");
    expect(init?.credentials).toBe("include");
    expect(headers.get("Accept")).toBe("application/json");
    expect(headers.get("X-Client")).toBe("frontend");
    expect(headers.get(CORRELATION_HEADER)).toMatch(/^[0-9a-f-]{36}$/);
  });

  it("preserves caller correlation", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({}, 200, "caller-123"));
    vi.stubGlobal("fetch", fetchMock);
    await requestJson("/technical/example", { headers: { [CORRELATION_HEADER]: "caller-123" } });
    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).get(CORRELATION_HEADER)).toBe("caller-123");
  });

  it("maps the backend error contract", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(
      { code: "VALIDATION_ERROR", message: "Solicitud inválida.", correlationId: "body" }, 400, "header",
    )));
    await expect(requestJson("/technical/example")).rejects.toMatchObject({
      code: "VALIDATION_ERROR", message: "Solicitud inválida.", status: 400, correlationId: "header",
    });
  });

  it("uses a generic network error without exposing native details", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockRejectedValue(new Error("secret socket detail")));
    const request = requestJson("/technical/example");
    await expect(request).rejects.toBeInstanceOf(HttpClientError);
    await expect(request).rejects.toMatchObject({ code: "NETWORK_ERROR" });
  });

  it("classifies its timeout independently", async () => {
    vi.stubGlobal("fetch", abortableFetch());
    await expect(requestJson("/slow", {}, { timeoutMs: 5 })).rejects.toMatchObject({ code: "TIMEOUT" });
  });

  it("respects caller cancellation", async () => {
    vi.stubGlobal("fetch", abortableFetch());
    const controller = new AbortController();
    const request = requestJson("/cancel", { signal: controller.signal });
    controller.abort();
    await expect(request).rejects.toMatchObject({ code: "REQUEST_ABORTED" });
  });

  it("rejects invalid successful JSON", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockResolvedValue(
      new Response("not-json", { status: 200, headers: { [CORRELATION_HEADER]: "invalid-json" } }),
    ));
    await expect(requestJson("/technical/example")).rejects.toMatchObject({
      code: "INVALID_RESPONSE", correlationId: "invalid-json",
    });
  });

  it("accepts an empty successful response", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockResolvedValue(new Response(null, { status: 204 })));
    await expect(requestJson("/empty")).resolves.toEqual({ data: undefined, correlationId: undefined });
  });

  it("uses a generic HTTP error for a non-JSON error", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockResolvedValue(new Response("gateway detail", { status: 502 })));
    await expect(requestJson("/technical/example")).rejects.toMatchObject({ code: "HTTP_ERROR", status: 502 });
  });

  it("coordinates one refresh and retries the protected request once", async () => {
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ code: "SESSION_EXPIRED" }, 401))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse({ nextStep: "IDENTITY_VERIFICATION" }, 200));
    vi.stubGlobal("fetch", fetchMock);

    await expect(requestJson<{ nextStep: string }>("/api/v1/session/current"))
      .resolves.toMatchObject({ data: { nextStep: "IDENTITY_VERIFICATION" } });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(String(fetchMock.mock.calls[1][0])).toContain("/api/v1/session/refresh");
    expect(fetchMock.mock.calls[1][1]?.credentials).toBe("include");
  });

  it("rejects a malformed current-session contract", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      sessionId: 1,
      requestId: 2,
      maskedDni: "******01",
      sessionStatus: "PENDING_IDENTITY",
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
      nextStep: "IDENTITY_VERIFICATION",
    }, 200, "session-correlation")));

    await expect(getCurrentFlowSession()).rejects.toMatchObject({
      code: "INVALID_RESPONSE",
      correlationId: "session-correlation",
    });
  });
});

function abortableFetch() {
  return vi.fn<typeof fetch>((_input, init) => new Promise((_resolve, reject) => {
    init?.signal?.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")), { once: true });
  }));
}

function jsonResponse(body: unknown, status: number, correlationId?: string): Response {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (correlationId) headers.set(CORRELATION_HEADER, correlationId);
  return new Response(JSON.stringify(body), { status, headers });
}
