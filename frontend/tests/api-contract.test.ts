import { describe, expect, it } from "vitest";

import type { ApiError, SystemStatus } from "@/lib/api/contracts";
import { SYSTEM_STATUS_PATH } from "@/lib/api/contracts";

describe("generated API aliases", () => {
  it("compile from generated schemas and expose the generated path", () => {
    const status: SystemStatus = { status: "UP", database: "UP", timestamp: "2026-07-16T12:00:00Z" };
    const error: ApiError = { code: "DEPENDENCY_UNAVAILABLE", message: "No disponible" };
    expect(status.database).toBe("UP");
    expect(error.code).toBe("DEPENDENCY_UNAVAILABLE");
    expect(SYSTEM_STATUS_PATH).toBe("/api/v1/system/status");
  });
});
