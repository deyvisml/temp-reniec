import { HttpClientError, requestJson } from "@/lib/http-client";
import type { CurrentFlowSessionContract } from "@/lib/api/contracts";

export type CurrentFlowSession = Omit<Required<CurrentFlowSessionContract>, "sessionStatus" | "nextStep"> & {
  sessionId: number;
  requestId: number;
  dni: string;
  sessionStatus: "PENDING_IDENTITY" | "IDENTITY_VERIFIED";
  requestStatus: string;
  nextStep: "IDENTITY_VERIFICATION" | "CERTIFICATE_SELECTION" | "CONFIRMATION" | "RECEIPT";
};

const CURRENT_SESSION_PATH = "/api/v1/session/current";
const LOGOUT_SESSION_PATH = "/api/v1/session/logout";

export function parseCurrentFlowSession(value: unknown): CurrentFlowSession | null {
  if (!value || typeof value !== "object") return null;

  const session = value as Record<string, unknown>;
  if (
    typeof session.sessionId !== "number" ||
    typeof session.requestId !== "number" ||
    typeof session.dni !== "string" ||
    !/^\d{8}$/.test(session.dni) ||
    (session.sessionStatus !== "PENDING_IDENTITY" && session.sessionStatus !== "IDENTITY_VERIFIED") ||
    typeof session.requestStatus !== "string" ||
    (session.nextStep !== "IDENTITY_VERIFICATION" && session.nextStep !== "CERTIFICATE_SELECTION"
      && session.nextStep !== "CONFIRMATION" && session.nextStep !== "RECEIPT")
  ) {
    return null;
  }

  return session as CurrentFlowSession;
}

export async function getCurrentFlowSession(signal?: AbortSignal) {
  const result = await requestJson<unknown>(CURRENT_SESSION_PATH, { signal });
  const session = parseCurrentFlowSession(result.data);
  if (!session) {
    throw new HttpClientError("El servicio devolvió una sesión no válida.", {
      code: "INVALID_RESPONSE",
      correlationId: result.correlationId,
    });
  }
  return { ...result, data: session };
}

export const logoutFlowSession = () =>
  requestJson<void>(LOGOUT_SESSION_PATH, { method: "POST" });
