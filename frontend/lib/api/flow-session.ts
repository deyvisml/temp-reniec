import { requestJson } from "@/lib/http-client";
import type { CurrentFlowSessionContract } from "@/lib/api/contracts";

export type CurrentFlowSession = Omit<Required<CurrentFlowSessionContract>, "sessionStatus" | "nextStep"> & {
  sessionId: number;
  requestId: number;
  dni: string;
  sessionStatus: "PENDING_IDENTITY" | "IDENTITY_VERIFIED";
  requestStatus: string;
  nextStep: "IDENTITY_VERIFICATION" | "CERTIFICATE_SELECTION" | "REASON";
};

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
      && session.nextStep !== "REASON")
  ) {
    return null;
  }

  return session as CurrentFlowSession;
}

export const logoutFlowSession = () =>
  requestJson<void>(LOGOUT_SESSION_PATH, { method: "POST" });
