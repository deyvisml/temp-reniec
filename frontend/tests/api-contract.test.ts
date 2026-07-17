import { describe, expect, it } from "vitest";

import type { ApiError, CancellationRequestResponse, StartCancellationRequest, SystemStatus } from "@/lib/api/contracts";
import { CANCELLATION_REQUESTS_PATH, SYSTEM_STATUS_PATH } from "@/lib/api/contracts";

describe("generated API aliases", () => {
  it("compile from generated schemas and expose the generated path", () => {
    const status: SystemStatus = { status: "UP", database: "UP", timestamp: "2026-07-16T12:00:00Z" };
    const error: ApiError = { code: "DEPENDENCY_UNAVAILABLE", message: "No disponible" };
    expect(status.database).toBe("UP");
    expect(error.code).toBe("DEPENDENCY_UNAVAILABLE");
    expect(SYSTEM_STATUS_PATH).toBe("/api/v1/system/status");
    const request: StartCancellationRequest = { dni: "00000001" };
    const response: CancellationRequestResponse = { requestId: 42, eligibilityResult: "ELIGIBLE", canContinue: true };
    expect(request.dni).toHaveLength(8);
    expect(response.canContinue).toBe(true);
    expect(response.requestId).toBe(42);
    expect(CANCELLATION_REQUESTS_PATH).toBe("/api/v1/cancellation-requests");
  });
});
