import { requestJson } from "@/lib/http-client";
import {
  REVOCATION_CONFIRMATION_PATH,
  REVOCATION_EXECUTION_PATH,
  REVOCATION_OUTCOME_PATH,
  REVOCATION_RECEIPT_RETRY_PATH,
  REVOCATION_REVIEW_PATH,
  type RevocationConfirmationContract,
  type RevocationReviewContract,
  type RevocationReviewRequestContract,
  type RevocationExecutionContract,
} from "@/lib/api/contracts";

export type RevocationReasonCode = RevocationReviewRequestContract["reasonCode"];

export type RevocationDraft = {
  digitalCredentialUuid: string | null;
  statusListIndex: number | null;
  reasonCode: RevocationReasonCode | null;
  otherReason: string;
};

export type CompleteRevocationDraft = RevocationReviewRequestContract;
export type RevocationReview = RevocationReviewContract;
export type RevocationExecution = RevocationExecutionContract;

let confirmedReviewRequest: ReturnType<typeof requestJson<RevocationReview>> | undefined;
let outcomeRequest: ReturnType<typeof requestJson<RevocationExecution>> | undefined;
let executionRequest: ReturnType<typeof requestJson<RevocationExecution>> | undefined;
const previewReviewRequests = new Map<
  string,
  ReturnType<typeof requestJson<RevocationReview>>
>();

export function getConfirmedRevocationReview() {
  confirmedReviewRequest ??= requestJson<RevocationReview>(REVOCATION_REVIEW_PATH)
    .finally(() => { confirmedReviewRequest = undefined; });
  return confirmedReviewRequest;
}

export function previewCurrentRevocation(draft: CompleteRevocationDraft) {
  const body = JSON.stringify(draft satisfies RevocationReviewRequestContract);
  const existingRequest = previewReviewRequests.get(body);
  if (existingRequest) return existingRequest;

  const request = requestJson<RevocationReview>(REVOCATION_REVIEW_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
  }).finally(() => { previewReviewRequests.delete(body); });
  previewReviewRequests.set(body, request);
  return request;
}

export const confirmCurrentRevocation = (
  draft: CompleteRevocationDraft,
  consentVersion: string,
  signal?: AbortSignal,
) =>
  requestJson<RevocationExecution>(REVOCATION_CONFIRMATION_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      ...draft,
      consentAccepted: true,
      consentVersion,
    } satisfies RevocationConfirmationContract),
    signal,
  }, { timeoutMs: 20_000 });

export function getCurrentRevocationOutcome() {
  outcomeRequest ??= requestJson<RevocationExecution>(REVOCATION_OUTCOME_PATH)
    .finally(() => { outcomeRequest = undefined; });
  return outcomeRequest;
}

export function resumeCurrentRevocationExecution() {
  executionRequest ??= requestJson<RevocationExecution>(
    REVOCATION_EXECUTION_PATH, { method: "POST" }, { timeoutMs: 20_000 },
  ).finally(() => { executionRequest = undefined; });
  return executionRequest;
}

export const retryCurrentRevocationReceipt = () =>
  requestJson<RevocationExecution>(REVOCATION_RECEIPT_RETRY_PATH, { method: "POST" }, { timeoutMs: 20_000 });
