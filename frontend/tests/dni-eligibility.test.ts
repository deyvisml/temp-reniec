import { afterEach, describe, expect, it, vi } from "vitest";

import { buildIdentityPath, validateDni } from "@/components/dni-eligibility-form";
import { startCancellationRequest } from "@/lib/api/cancellation-requests";

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
      reused: false,
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
});
