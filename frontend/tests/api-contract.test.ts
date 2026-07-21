import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

import type { ApiError, CancellationRequestResponse, StartCancellationRequest, SystemStatus } from "@/lib/api/contracts";
import { CANCELLATION_REQUESTS_PATH, SYSTEM_STATUS_PATH } from "@/lib/api/contracts";

describe("generated API aliases", () => {
  it("compile from generated schemas and expose the generated path", () => {
    const status: SystemStatus = { status: "UP", database: "UP", timestamp: "2026-07-16T12:00:00Z" };
    const error: ApiError = {
      code: "DEPENDENCY_UNAVAILABLE",
      correlationId: "contract-test",
      message: "No disponible",
      path: "/api/v1/system/status",
      timestamp: "2026-07-16T12:00:00Z",
    };
    expect(status.database).toBe("UP");
    expect(error.code).toBe("DEPENDENCY_UNAVAILABLE");
    expect(SYSTEM_STATUS_PATH).toBe("/api/v1/system/status");
    const request: StartCancellationRequest = { dni: "00000001" };
    const response: CancellationRequestResponse = {
      canContinue: true,
      availabilityResult: "AVAILABLE",
      maskedDni: "******01",
      nextStep: "IDENTITY_VERIFICATION",
      requestId: 42,
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
    };
    expect(request.dni).toHaveLength(8);
    expect(response.canContinue).toBe(true);
    expect(response.requestId).toBe(42);
    expect(CANCELLATION_REQUESTS_PATH).toBe("/api/v1/cancellation-requests");
  });

  it("keeps the committed contract free of progress-recovery semantics", () => {
    const openApi = readFileSync(join(process.cwd(), "openapi", "backend-api.json"), "utf8");
    const generated = readFileSync(join(process.cwd(), "lib", "api", "generated.ts"), "utf8");

    expect(openApi).toContain("CANCELLATION_REQUEST_IN_PROGRESS");
    expect(openApi).not.toMatch(/reused|publicReference|recupera una solicitud|inicio o recuperación/i);
    expect(generated).not.toMatch(/reused|publicReference|recupera una solicitud|inicio o recuperación/i);
  });

  it("keeps the initial response limited to certificate existence", () => {
    const openApi = JSON.parse(
      readFileSync(join(process.cwd(), "openapi", "backend-api.json"), "utf8"),
    );
    const schema = openApi.components.schemas.CancellationRequestResponse;

    expect(schema.required).toContain("availabilityResult");
    expect(schema.properties.availabilityResult.enum).toEqual([
      "AVAILABLE",
      "NOT_AVAILABLE",
      "INCONCLUSIVE",
      "UNAVAILABLE",
      "ERROR",
    ]);
    expect(Object.keys(schema.properties)).not.toEqual(
      expect.arrayContaining([
        "eligibilityResult",
        "certificates",
        "certificateCount",
        "orderNumber",
        "emissionCreatedAt",
        "certificateUuid",
      ]),
    );
  });
});
