import { NextRequest, NextResponse } from "next/server";
import { resolveBackendUrl } from "@/lib/http-client";

const ACCESS_COOKIE = "cancelacion_access";
const REFRESH_COOKIE = "cancelacion_refresh";

export async function GET(request: NextRequest) {
  const requestedReturn = request.nextUrl.searchParams.get("returnTo") ?? "/cancelacion";
  const returnTo = requestedReturn.startsWith("/") && !requestedReturn.startsWith("//")
    ? requestedReturn
    : "/cancelacion";
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;

  if (!refreshToken) return clearSessionAndRedirectHome(request);

  try {
    const backend = await fetch(new URL("/api/v1/session/refresh", resolveBackendUrl("server")), {
      method: "POST",
      headers: {
        Accept: "application/json",
        Cookie: `${REFRESH_COOKIE}=${refreshToken}`,
        Origin: request.nextUrl.origin,
      },
      cache: "no-store",
    });
    if (backend.ok) {
      const response = NextResponse.redirect(new URL(returnTo, request.url));
      for (const cookie of backend.headers.getSetCookie()) response.headers.append("Set-Cookie", cookie);
      return response;
    }
  } catch {
    // A failed refresh is handled as an ended session below.
  }

  return clearSessionAndRedirectHome(request);
}

function clearSessionAndRedirectHome(request: NextRequest) {
  const response = NextResponse.redirect(new URL("/", request.url));
  response.cookies.delete(ACCESS_COOKIE);
  response.cookies.delete(REFRESH_COOKIE);
  return response;
}
