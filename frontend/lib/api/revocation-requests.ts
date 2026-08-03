import {
  REVOCATION_REQUESTS_PATH,
  type RevocationRequestResponse,
  type StartRevocationRequest,
} from "@/lib/api/contracts";
import { requestJson, type HttpResult } from "@/lib/http-client";

export function startRevocationRequest(
  dni: string,
  recaptchaToken: string,
  signal?: AbortSignal,
): Promise<HttpResult<RevocationRequestResponse>> {
  const body: StartRevocationRequest = { dni, recaptchaToken };
  return requestJson<RevocationRequestResponse>(REVOCATION_REQUESTS_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    signal,
  });
}
