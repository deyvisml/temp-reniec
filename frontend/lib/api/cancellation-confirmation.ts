import { requestJson } from "@/lib/http-client";
import {
  CANCELLATION_CONFIRMATION_PATH,
  CANCELLATION_EXECUTION_PATH,
  CANCELLATION_OUTCOME_PATH,
  CANCELLATION_RECEIPT_RETRY_PATH,
  CANCELLATION_REVIEW_PATH,
  type CancellationConfirmationContract,
  type CancellationReviewContract,
  type CancellationReviewRequestContract,
  type CancellationExecutionContract,
} from "@/lib/api/contracts";

export type CancellationReasonCode = CancellationReviewRequestContract["reasonCode"];

export type CancellationDraft = {
  certificateUuid: string | null;
  reasonCode: CancellationReasonCode | null;
  otherReason: string;
};

export type CompleteCancellationDraft = CancellationReviewRequestContract;
export type CancellationReview = CancellationReviewContract;
export type CancellationExecution = CancellationExecutionContract;

let confirmedReviewRequest: ReturnType<typeof requestJson<CancellationReview>> | undefined;
let outcomeRequest: ReturnType<typeof requestJson<CancellationExecution>> | undefined;
let executionRequest: ReturnType<typeof requestJson<CancellationExecution>> | undefined;
const previewReviewRequests = new Map<
  string,
  ReturnType<typeof requestJson<CancellationReview>>
>();

export function getConfirmedCancellationReview() {
  confirmedReviewRequest ??= requestJson<CancellationReview>(CANCELLATION_REVIEW_PATH)
    .finally(() => { confirmedReviewRequest = undefined; });
  return confirmedReviewRequest;
}

export function previewCurrentCancellation(draft: CompleteCancellationDraft) {
  const body = JSON.stringify(draft satisfies CancellationReviewRequestContract);
  const existingRequest = previewReviewRequests.get(body);
  if (existingRequest) return existingRequest;

  const request = requestJson<CancellationReview>(CANCELLATION_REVIEW_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
  }).finally(() => { previewReviewRequests.delete(body); });
  previewReviewRequests.set(body, request);
  return request;
}

export const confirmCurrentCancellation = (
  draft: CompleteCancellationDraft,
  consentVersion: string,
  signal?: AbortSignal,
) =>
  requestJson<CancellationExecution>(CANCELLATION_CONFIRMATION_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      ...draft,
      consentAccepted: true,
      consentVersion,
    } satisfies CancellationConfirmationContract),
    signal,
  }, { timeoutMs: 20_000 });

export function getCurrentCancellationOutcome() {
  outcomeRequest ??= requestJson<CancellationExecution>(CANCELLATION_OUTCOME_PATH)
    .finally(() => { outcomeRequest = undefined; });
  return outcomeRequest;
}

export function resumeCurrentCancellationExecution() {
  executionRequest ??= requestJson<CancellationExecution>(
    CANCELLATION_EXECUTION_PATH, { method: "POST" }, { timeoutMs: 20_000 },
  ).finally(() => { executionRequest = undefined; });
  return executionRequest;
}

export const retryCurrentCancellationReceipt = () =>
  requestJson<CancellationExecution>(CANCELLATION_RECEIPT_RETRY_PATH, { method: "POST" }, { timeoutMs: 20_000 });
