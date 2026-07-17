import { describe, expect, it } from "vitest";

import { getSystemStatus } from "@/lib/api/system-status";
import { startCancellationRequest } from "@/lib/api/cancellation-requests";

describe("live frontend to backend integration", () => {
  it("reaches the backend and its MySQL datasource with correlation", async () => {
    const result = await getSystemStatus();
    expect(result.data.status).toBe("UP");
    expect(result.data.database).toBe("UP");
    expect(result.data.timestamp).toBeTruthy();
    expect(result.correlationId).toBeTruthy();
  });

  it("initiates a real eligibility request through backend and MySQL", async () => {
    const result = await startCancellationRequest("00000001");
    expect(result.data?.eligibilityResult).toBe("ELIGIBLE");
    expect(result.data?.requestStatus).toBe("PENDING_IDENTITY_VERIFICATION");
    expect(result.data?.canContinue).toBe(true);
    expect(result.data?.nextStep).toBe("IDENTITY_VERIFICATION");
    expect(result.data?.maskedDni).toBe("******01");
    expect(result.data?.requestId).toBeTypeOf("number");
    expect(result.correlationId).toBeTruthy();
    expect(JSON.stringify(result.data)).not.toContain("00000001");
  });
});
