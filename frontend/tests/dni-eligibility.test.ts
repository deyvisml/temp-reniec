import { readFileSync } from "node:fs";
import { join } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";

import { buildEligibilityErrorView, buildIdentityPath, validateDni } from "@/components/dni-eligibility-form";
import { startCancellationRequest } from "@/lib/api/cancellation-requests";
import { HttpClientError } from "@/lib/http-client";

afterEach(() => vi.unstubAllGlobals());

describe("DNI eligibility entry", () => {
  it("accepts only eight ASCII digits", () => {
    expect(validateDni("12345678")).toBeUndefined();
    expect(validateDni("")).toContain("Ingresa");
    expect(validateDni("1234567")).toContain("8 dígitos");
    expect(validateDni("123456789")).toContain("8 dígitos");
    expect(validateDni("1234A678")).toContain("8 dígitos");
    expect(validateDni("１２３４５６７８")).toContain("8 dígitos");
  });

  it("builds the next path with only the request id", () => {
    const path = buildIdentityPath(42);
    expect(path).toBe("/verificacion-identidad?requestId=42");
    expect(path).not.toContain("12345678");
  });

  it("uses the shared correlated JSON transport", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      requestId: 42,
      maskedDni: "******01",
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
      eligibilityResult: "ELIGIBLE",
      canContinue: true,
      nextStep: "IDENTITY_VERIFICATION",
    }), { status: 200, headers: { "Content-Type": "application/json", "X-Correlation-ID": "front-test" } }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await startCancellationRequest("00000001");

    expect(result.data?.canContinue).toBe(true);
    expect(result.correlationId).toBe("front-test");
    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(url.pathname).toBe("/api/v1/cancellation-requests");
    expect(init.method).toBe("POST");
    expect(init.body).toBe('{"dni":"00000001"}');
  });

  it("maps a protected prior operation to a generic non-retryable result", () => {
    const view = buildEligibilityErrorView(new HttpClientError("Conflict", {
      code: "CANCELLATION_REQUEST_IN_PROGRESS",
      correlationId: "protected-test",
    }));

    expect(view.retryable).toBe(false);
    expect(view.correlationId).toBe("protected-test");
    expect(`${view.title} ${view.message}`).not.toMatch(/requestId|constancia|selección anterior|recuper/i);
  });

  it("does not restore DNI or request progress from browser storage", () => {
    const source = readFileSync(join(process.cwd(), "components", "dni-eligibility-form.tsx"), "utf8");

    expect(source).not.toMatch(/localStorage|sessionStorage/);
    expect(source).not.toMatch(/restore|rehydrat|reanud|recuperar/i);
  });
});
