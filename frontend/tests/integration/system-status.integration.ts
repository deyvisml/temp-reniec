import { describe, expect, it } from "vitest";

import { getSystemStatus } from "@/lib/api/system-status";
import { startRevocationRequest } from "@/lib/api/revocation-requests";

describe("live frontend to backend integration", () => {
  const recaptchaToken = process.env.RECAPTCHA_TEST_TOKEN ?? "test-recaptcha-valid";
  it("reaches the backend and its MySQL datasource with correlation", async () => {
    const result = await getSystemStatus();
    expect(result.data.status).toBe("UP");
    expect(result.data.database).toBe("UP");
    expect(result.data.timestamp).toBeTruthy();
    expect(result.correlationId).toBeTruthy();
  });

	it("initiates a real positive availability request through backend and MySQL", async () => {
    const result = await startRevocationRequest("00000001", recaptchaToken);
    expect(result.data?.availabilityResult).toBe("AVAILABLE");
    expect(result.data?.requestStatus).toBe("PENDING_IDENTITY_VERIFICATION");
    expect(result.data?.canContinue).toBe(true);
    expect(result.data?.nextStep).toBe("IDENTITY_VERIFICATION");
    expect(result.data?.maskedDni).toBe("******01");
    expect(result.data?.requestId).toBeTypeOf("number");
    expect(result.correlationId).toBeTruthy();
    expect(JSON.stringify(result.data)).not.toContain("00000001");
		expect(JSON.stringify(result.data)).not.toMatch(
			/digitalCredentialUuid|orderNumber|emissionCreatedAt|digitalCredentialCount|digitalCredentials/,
		);
  });

	it("keeps a confirmed negative availability result blocked", async () => {
		const result = await startRevocationRequest("00000002", recaptchaToken);
		expect(result.data?.availabilityResult).toBe("NOT_AVAILABLE");
		expect(result.data?.requestStatus).toBe("NO_DIGITAL_CREDENTIALS_AVAILABLE");
		expect(result.data?.canContinue).toBe(false);
		expect(result.data?.nextStep).toBeNull();
		expect(JSON.stringify(result.data)).not.toContain("00000002");
	});
});
