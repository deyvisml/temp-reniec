import { describe, expect, it } from "vitest";

import { getSystemStatus } from "@/lib/api/system-status";

describe("live frontend to backend integration", () => {
  it("reaches the backend and its MySQL datasource with correlation", async () => {
    const result = await getSystemStatus();
    expect(result.data.status).toBe("UP");
    expect(result.data.database).toBe("UP");
    expect(result.data.timestamp).toBeTruthy();
    expect(result.correlationId).toBeTruthy();
  });
});
