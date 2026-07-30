import { requestJson } from "@/lib/http-client";
import {
  CANCELLATION_REASON_PATH,
  type CancellationReasonContract,
  type CancellationReasonRequestContract,
} from "@/lib/api/contracts";

export type CancellationReasonCode = "THEFT" | "LOSS" | "DEVICE_OR_NUMBER_CHANGE" | "SUSPECTED_UNAUTHORIZED_USE" | "OTHER";
export type CancellationReason = CancellationReasonContract;

let currentCancellationReasonRequest: ReturnType<typeof requestJson<CancellationReason>> | undefined;

/** Comparte solo la lectura activa para evitar cargas duplicadas al remontar la vista. */
export function getCurrentCancellationReason() {
  currentCancellationReasonRequest ??= requestJson<CancellationReason>(CANCELLATION_REASON_PATH)
    .finally(() => { currentCancellationReasonRequest = undefined; });
  return currentCancellationReasonRequest;
}

export const saveCurrentCancellationReason = (reasonCode: CancellationReasonCode, otherReason: string | null) =>
  requestJson<CancellationReason>(CANCELLATION_REASON_PATH, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reasonCode, otherReason: otherReason ?? undefined } satisfies CancellationReasonRequestContract),
  });
