import { readFileSync } from "node:fs";
import { join } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  buildAvailabilityErrorView,
  buildRecaptchaErrorMessage,
  canSubmitInitialQuery,
  isConsistentInitialResponse,
  validateDni,
} from "@/components/dni-availability-form";
import type { CancellationRequestResponse } from "@/lib/api/contracts";
import { startCancellationRequest } from "@/lib/api/cancellation-requests";
import { HttpClientError } from "@/lib/http-client";
import { CANCELLATION_FLOW_ROUTE } from "@/lib/routes";

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

  it("uses one canonical route without exposing request identifiers", () => {
    expect(CANCELLATION_FLOW_ROUTE).toBe("/cancelacion");
    expect(CANCELLATION_FLOW_ROUTE).not.toContain("42");
    expect(CANCELLATION_FLOW_ROUTE).not.toContain("12345678");
  });

  it("rejects inconsistent or privacy-unsafe initial responses", () => {
    const valid: CancellationRequestResponse = {
      requestId: 42,
      maskedDni: "******01",
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
      availabilityResult: "AVAILABLE",
      canContinue: true,
      nextStep: "IDENTITY_VERIFICATION",
    };

    expect(isConsistentInitialResponse(valid)).toBe(true);
    expect(isConsistentInitialResponse({ ...valid, requestId: 0 })).toBe(false);
    expect(isConsistentInitialResponse({ ...valid, maskedDni: "00000001" })).toBe(false);
    expect(isConsistentInitialResponse({ ...valid, canContinue: false })).toBe(false);
    expect(isConsistentInitialResponse({ ...valid, nextStep: null })).toBe(false);
    expect(isConsistentInitialResponse({
      ...valid,
      availabilityResult: "NOT_AVAILABLE",
      requestStatus: "NO_CERTIFICATES_AVAILABLE",
      canContinue: false,
      nextStep: null,
    })).toBe(true);
  });

  it("uses the shared correlated JSON transport", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      requestId: 42,
      maskedDni: "******01",
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
      availabilityResult: "AVAILABLE",
      canContinue: true,
      nextStep: "IDENTITY_VERIFICATION",
    }), { status: 200, headers: { "Content-Type": "application/json", "X-Correlation-ID": "front-test" } }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await startCancellationRequest("00000001", "ephemeral-test-token");

    expect(result.data?.canContinue).toBe(true);
    expect(result.correlationId).toBe("front-test");
		expect(JSON.stringify(result.data)).not.toMatch(
			/certificateUuid|orderNumber|emissionCreatedAt|certificateCount|00000001/,
		);
    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(url.pathname).toBe("/api/v1/cancellation-requests");
    expect(init.method).toBe("POST");
    expect(init.body).toBe('{"dni":"00000001","recaptchaToken":"ephemeral-test-token"}');
  });

  it("requires valid DNI, configured widget and current token before submission", () => {
    expect(canSubmitInitialQuery("00000001", "token", false, true)).toBe(true);
    expect(canSubmitInitialQuery("0000000", "token", false, true)).toBe(false);
    expect(canSubmitInitialQuery("00000001", "", false, true)).toBe(false);
    expect(canSubmitInitialQuery("00000001", "token", true, true)).toBe(false);
    expect(canSubmitInitialQuery("00000001", "token", false, false)).toBe(false);
  });

  it("maps CAPTCHA failures separately from certificate availability", () => {
    expect(buildRecaptchaErrorMessage(new HttpClientError("Rejected", {
      code: "RECAPTCHA_REJECTED",
    }))).toContain("verificación");
    expect(buildRecaptchaErrorMessage(new HttpClientError("Expired", {
      code: "RECAPTCHA_EXPIRED_OR_DUPLICATE",
    }))).toContain("expiró");
    expect(buildRecaptchaErrorMessage(new HttpClientError("Availability", {
      code: "AVAILABILITY_UNAVAILABLE",
    }))).toBeUndefined();
  });

  it("maps a protected prior operation to a generic non-retryable result", () => {
    const view = buildAvailabilityErrorView(new HttpClientError("Conflict", {
      code: "CANCELLATION_REQUEST_IN_PROGRESS",
      correlationId: "protected-test",
    }));

    expect(view.retryable).toBe(false);
    expect(view.correlationId).toBe("protected-test");
    expect(`${view.title} ${view.message}`).not.toMatch(/requestId|constancia|selección anterior|recuper/i);
  });

  it("does not restore DNI or request progress from browser storage", () => {
    const source = readFileSync(join(process.cwd(), "components", "dni-availability-form.tsx"), "utf8");

    expect(source).not.toMatch(/localStorage|sessionStorage/);
    expect(source).not.toMatch(/document\.cookie|console\.(log|info|debug)/);
    expect(source).not.toMatch(/restore|rehydrat|reanud|recuperar/i);
    expect(source).toContain("submissionInFlightRef.current");
    expect(source).toContain("onContinue()");
    expect(source).not.toContain("window.location.assign");
    expect(source).toContain("setRecaptchaToken(\"\")");
    expect(source).toContain("setRecaptchaResetKey");
  });
});
