import {
  CANCELLATION_REQUESTS_PATH,
  type CancellationRequestResponse,
  type StartCancellationRequest,
} from "@/lib/api/contracts";
import { requestJson, type HttpResult } from "@/lib/http-client";

export function startCancellationRequest(
  dni: string,
  signal?: AbortSignal,
): Promise<HttpResult<CancellationRequestResponse>> {
  const body: StartCancellationRequest = { dni };
  return requestJson<CancellationRequestResponse>(CANCELLATION_REQUESTS_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    signal,
  });
}
