import { requestJson } from "@/lib/http-client";
import {
  CANCELLATION_CONFIRMATION_PATH,
  CANCELLATION_REVIEW_PATH,
  type CancellationConfirmationContract,
  type CancellationReviewContract,
} from "@/lib/api/contracts";

export type CancellationReview = CancellationReviewContract;

export const getCurrentCancellationReview = (signal?: AbortSignal) =>
  requestJson<CancellationReview>(CANCELLATION_REVIEW_PATH, { signal });

export const confirmCurrentCancellation = (consentVersion: string, signal?: AbortSignal) =>
  requestJson<CancellationReview>(CANCELLATION_CONFIRMATION_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      consentAccepted: true,
      consentVersion,
    } satisfies CancellationConfirmationContract),
    signal,
  });
