import type { components, paths } from "@/lib/api/generated";

export type SystemStatus = components["schemas"]["SystemStatusResponse"];
export type ApiError = components["schemas"]["ApiError"];
export type StartRevocationRequest = components["schemas"]["StartRevocationRequest"];
export type RevocationRequestResponse = components["schemas"]["RevocationRequestResponse"];
export type IdentityStartResponse = components["schemas"]["IdentityStartResponse"];
export type CurrentIdentityResponse = components["schemas"]["CurrentIdentityResponse"];
export type CurrentFlowSessionContract = components["schemas"]["CurrentSession"];
export type DigitalCredentialItemContract = components["schemas"]["DigitalCredentialItem"];
export type DigitalCredentialListContract = components["schemas"]["DigitalCredentialListResponse"];
export type RevocationReviewContract = components["schemas"]["RevocationReviewResponse"];
export type RevocationReviewRequestContract = components["schemas"]["RevocationReviewRequest"];
export type RevocationConfirmationContract = components["schemas"]["RevocationConfirmationRequest"];
export type RevocationExecutionContract = components["schemas"]["RevocationExecutionResponse"];

export const SYSTEM_STATUS_PATH = "/api/v1/system/status" satisfies keyof paths;
export const REVOCATION_REQUESTS_PATH = "/api/v1/revocation-requests" satisfies keyof paths;
export const IDENTITY_VERIFICATIONS_PATH = "/api/v1/identity-verifications" satisfies keyof paths;
export const CURRENT_IDENTITY_PATH = "/api/v1/identity-verifications/current" satisfies keyof paths;
export const CURRENT_DIGITAL_CREDENTIALS_PATH = "/api/v1/revocation-requests/current/digital-credentials" satisfies keyof paths;
export const REVOCATION_REVIEW_PATH = "/api/v1/revocation-requests/current/review" satisfies keyof paths;
export const REVOCATION_CONFIRMATION_PATH = "/api/v1/revocation-requests/current/confirmation" satisfies keyof paths;
export const REVOCATION_OUTCOME_PATH = "/api/v1/revocation-requests/current/outcome" satisfies keyof paths;
export const REVOCATION_EXECUTION_PATH = "/api/v1/revocation-requests/current/execution" satisfies keyof paths;
export const REVOCATION_RECEIPT_RETRY_PATH = "/api/v1/revocation-requests/current/receipt/retry" satisfies keyof paths;
export const REVOCATION_RECEIPT_PATH = "/api/v1/revocation-requests/current/receipt" satisfies keyof paths;
