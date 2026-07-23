import { requestJson } from "@/lib/http-client";
import {
  CURRENT_IDENTITY_PATH,
  IDENTITY_VERIFICATIONS_PATH,
  type CurrentIdentityResponse,
  type IdentityStartResponse,
} from "@/lib/api/contracts";

export async function startIdentityVerification(signal?: AbortSignal) {
  return requestJson<IdentityStartResponse>(IDENTITY_VERIFICATIONS_PATH, {
    method: "POST",
    signal,
  });
}

export async function getCurrentIdentityVerification(signal?: AbortSignal) {
  return requestJson<CurrentIdentityResponse>(CURRENT_IDENTITY_PATH, { signal });
}
