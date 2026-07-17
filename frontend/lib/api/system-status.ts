import type { SystemStatus } from "@/lib/api/contracts";
import { SYSTEM_STATUS_PATH } from "@/lib/api/contracts";
import { HttpClientError, requestJson } from "@/lib/http-client";

export async function getSystemStatus(signal?: AbortSignal) {
  const result = await requestJson<SystemStatus>(SYSTEM_STATUS_PATH, { signal });
  if (!result.data) {
    throw new HttpClientError("El servicio devolvió una respuesta no válida.", {
      code: "INVALID_RESPONSE",
      correlationId: result.correlationId,
    });
  }
  return { data: result.data, correlationId: result.correlationId };
}
