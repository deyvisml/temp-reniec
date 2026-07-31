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
export type CancellationReviewContract = components["schemas"]["CancellationReviewResponse"];
export type CancellationReviewRequestContract = components["schemas"]["CancellationReviewRequest"];
export type CancellationConfirmationContract = components["schemas"]["CancellationConfirmationRequest"];
export type CancellationExecutionContract = components["schemas"]["CancellationExecutionResponse"];

export const SYSTEM_STATUS_PATH = "/api/v1/system/status" satisfies keyof paths;
export const CANCELLATION_REQUESTS_PATH = "/api/v1/cancellation-requests" satisfies keyof paths;
export const IDENTITY_VERIFICATIONS_PATH = "/api/v1/identity-verifications" satisfies keyof paths;
export const CURRENT_IDENTITY_PATH = "/api/v1/identity-verifications/current" satisfies keyof paths;
export const CURRENT_CERTIFICATES_PATH = "/api/v1/cancellation-requests/current/certificates" satisfies keyof paths;
export const CANCELLATION_REVIEW_PATH = "/api/v1/cancellation-requests/current/review" satisfies keyof paths;
export const CANCELLATION_CONFIRMATION_PATH = "/api/v1/cancellation-requests/current/confirmation" satisfies keyof paths;
export const CANCELLATION_OUTCOME_PATH = "/api/v1/cancellation-requests/current/outcome" satisfies keyof paths;
export const CANCELLATION_EXECUTION_PATH = "/api/v1/cancellation-requests/current/execution" satisfies keyof paths;
export const CANCELLATION_RECEIPT_RETRY_PATH = "/api/v1/cancellation-requests/current/receipt/retry" satisfies keyof paths;
export const CANCELLATION_RECEIPT_PATH = "/api/v1/cancellation-requests/current/receipt" satisfies keyof paths;
