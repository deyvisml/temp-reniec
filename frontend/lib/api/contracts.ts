import type { components, paths } from "@/lib/api/generated";

export type SystemStatus = components["schemas"]["SystemStatusResponse"];
export type ApiError = components["schemas"]["ApiError"];
export type StartCancellationRequest = components["schemas"]["StartCancellationRequest"];
export type CancellationRequestResponse = components["schemas"]["CancellationRequestResponse"];
export type IdentityStartResponse = components["schemas"]["IdentityStartResponse"];
export type CurrentIdentityResponse = components["schemas"]["CurrentIdentityResponse"];
export type CurrentFlowSessionContract = components["schemas"]["CurrentSession"];
export type CertificateItemContract = components["schemas"]["CertificateItem"];
export type CertificateListContract = components["schemas"]["CertificateListResponse"];
export type CertificateSelectionContract = components["schemas"]["CertificateSelectionRequest"];

export const SYSTEM_STATUS_PATH = "/api/v1/system/status" satisfies keyof paths;
export const CANCELLATION_REQUESTS_PATH = "/api/v1/cancellation-requests" satisfies keyof paths;
export const IDENTITY_VERIFICATIONS_PATH = "/api/v1/identity-verifications" satisfies keyof paths;
export const CURRENT_IDENTITY_PATH = "/api/v1/identity-verifications/current" satisfies keyof paths;
export const CURRENT_CERTIFICATES_PATH = "/api/v1/cancellation-requests/current/certificates" satisfies keyof paths;
export const CERTIFICATE_SELECTION_PATH = "/api/v1/cancellation-requests/current/certificate-selection" satisfies keyof paths;
