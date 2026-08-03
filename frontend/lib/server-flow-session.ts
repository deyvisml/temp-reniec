import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { resolveBackendUrl } from "@/lib/http-client";
import { parseCurrentFlowSession, type CurrentFlowSession } from "@/lib/api/flow-session";

const CURRENT = "/api/v1/session/current";
const ACCESS_COOKIE = "revocacion_access";
const REFRESH_COOKIE = "revocacion_refresh";

export async function hasServerRefreshToken(): Promise<boolean> {
  return Boolean((await cookies()).get(REFRESH_COOKIE)?.value);
}

export async function readServerFlowSession(): Promise<CurrentFlowSession | null> {
  const store = await cookies();
  const accessToken = store.get(ACCESS_COOKIE)?.value;
  if (!accessToken) return null;
  try {
    const response = await fetch(new URL(CURRENT, resolveBackendUrl("server")), {
      headers: { Accept: "application/json", Cookie: `${ACCESS_COOKIE}=${accessToken}` },
      cache: "no-store",
    });
    return response.ok ? parseCurrentFlowSession(await response.json()) : null;
  } catch { return null; }
}

export async function requireServerFlowSession(returnTo: string) {
  const session = await readServerFlowSession();
  if (!session && await hasServerRefreshToken()) {
    redirect(`/api/session/refresh?returnTo=${encodeURIComponent(returnTo)}`);
  }
  if (!session) redirect("/");
  return session;
}
