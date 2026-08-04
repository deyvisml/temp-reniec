import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

import type { ApiError, RevocationRequestResponse, StartRevocationRequest, SystemStatus } from "@/lib/api/contracts";
import { REVOCATION_REQUESTS_PATH, SYSTEM_STATUS_PATH } from "@/lib/api/contracts";

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
    const request: StartRevocationRequest = { dni: "00000001", recaptchaToken: "ephemeral-test-token" };
    const response: RevocationRequestResponse = {
      canContinue: true,
      availabilityResult: "AVAILABLE",
      maskedDni: "******01",
      nextStep: "IDENTITY_VERIFICATION",
      requestId: 42,
      requestStatus: "PENDING_IDENTITY_VERIFICATION",
    };
    expect(request.dni).toHaveLength(8);
    expect(request.recaptchaToken).toBe("ephemeral-test-token");
    expect(response.canContinue).toBe(true);
    expect(response.requestId).toBe(42);
    expect(REVOCATION_REQUESTS_PATH).toBe("/api/v1/revocation-requests");
  });

  it("requires bounded write-only CAPTCHA evidence without reusable examples or secrets", () => {
    const openApi = JSON.parse(
      readFileSync(join(process.cwd(), "openapi", "backend-api.json"), "utf8"),
    );
    const schema = openApi.components.schemas.StartRevocationRequest;

    expect(schema.required).not.toContain("recaptchaToken");
    expect(schema.properties.recaptchaToken).toMatchObject({ writeOnly: true, maxLength: 4096 });
    expect(JSON.stringify(openApi)).not.toMatch(/test-recaptcha-valid|RECAPTCHA_SECRET_KEY|secretKey/);
  });

  it("keeps the committed contract free of progress-recovery semantics", () => {
    const openApi = readFileSync(join(process.cwd(), "openapi", "backend-api.json"), "utf8");
    const generated = readFileSync(join(process.cwd(), "lib", "api", "generated.ts"), "utf8");

    expect(openApi).toContain("REVOCATION_REQUEST_IN_PROGRESS");
    expect(openApi).not.toMatch(/reused|publicReference|recupera una solicitud|inicio o recuperación/i);
    expect(generated).not.toMatch(/reused|publicReference|recupera una solicitud|inicio o recuperación/i);
  });

  it("keeps the initial response limited to digitalCredential existence", () => {
    const openApi = JSON.parse(
      readFileSync(join(process.cwd(), "openapi", "backend-api.json"), "utf8"),
    );
    const schema = openApi.components.schemas.RevocationRequestResponse;

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
        "digitalCredentials",
        "digitalCredentialCount",
        "statusListIndex",
        "emissionCreatedAt",
        "digitalCredentialUuid",
      ]),
    );
  });

  it("persists the editable decision only through confirmation", () => {
    const openApi = JSON.parse(
      readFileSync(join(process.cwd(), "openapi", "backend-api.json"), "utf8"),
    );

    expect(openApi.paths["/api/v1/revocation-requests/current/digital-credential-selection"]).toBeUndefined();
    expect(openApi.paths["/api/v1/revocation-requests/current/reason"]).toBeUndefined();
    expect(openApi.paths["/api/v1/revocation-requests/current/review"].post).toBeDefined();

    const preview = openApi.components.schemas.RevocationReviewRequest;
    const confirmation = openApi.components.schemas.RevocationConfirmationRequest;
    for (const schema of [preview, confirmation]) {
      expect(schema.required).toEqual(expect.arrayContaining([
        "digitalCredentialUuid",
        "statusListIndex",
        "reasonCode",
      ]));
      expect(schema.properties.statusListIndex).toMatchObject({ type: "integer", minimum: 0 });
    }
    expect(confirmation.required).toEqual(
      expect.arrayContaining(["consentAccepted", "consentVersion"]),
    );

    const selected = openApi.components.schemas.SelectedDigitalCredential;
    expect(selected.properties).not.toHaveProperty("maskedUuid");
    expect(selected.properties).not.toHaveProperty("digitalCredentialUuid");

    const execution = openApi.components.schemas.RevocationExecutionResponse;
	const review = openApi.components.schemas.RevocationReviewResponse;
	expect(review.properties.firstName).toMatchObject({ type: "string", maxLength: 100 });
	expect(execution.properties.firstName).toMatchObject({ type: "string", maxLength: 100 });
	expect(review.required ?? []).not.toContain("firstName");
	expect(execution.required ?? []).not.toContain("firstName");
	expect(openApi.components.schemas.CurrentSession.properties).not.toHaveProperty("firstName");
	expect(openApi.components.schemas.DigitalCredentialListResponse.properties).not.toHaveProperty("firstName");
    expect(execution.required).toEqual(expect.arrayContaining([
      "state", "requestStatus", "maskedDni", "digitalCredential", "reasonLabel",
    ]));
    expect(execution.properties.state.enum).toEqual([
      "PROCESSING", "SUCCEEDED", "FAILED", "OUTCOME_UNKNOWN", "RECEIPT_FAILED",
    ]);
    expect(execution.properties.requestStatus.enum).toContain("RECEIPT_AVAILABLE");
    expect(openApi.paths["/api/v1/revocation-requests/current/confirmation"]
      .post.responses["503"]).toBeDefined();
  });
});
