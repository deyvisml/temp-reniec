import type { components, paths } from "@/lib/api/generated";

export type SystemStatus = components["schemas"]["SystemStatusResponse"];
export type ApiError = components["schemas"]["ApiError"];

export const SYSTEM_STATUS_PATH = "/api/v1/system/status" satisfies keyof paths;
