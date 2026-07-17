import type { components, paths } from "@/lib/api/generated";

export type SystemStatus = components["schemas"]["SystemStatusResponse"];
export type ApiError = components["schemas"]["ApiError"];
export type StartCancellationRequest = components["schemas"]["StartCancellationRequest"];
export type CancellationRequestResponse = components["schemas"]["CancellationRequestResponse"];

export const SYSTEM_STATUS_PATH = "/api/v1/system/status" satisfies keyof paths;
export const CANCELLATION_REQUESTS_PATH = "/api/v1/cancellation-requests" satisfies keyof paths;
