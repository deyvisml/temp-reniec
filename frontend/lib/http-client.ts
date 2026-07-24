const DEFAULT_BACKEND_URL = "http://localhost:8080";
export const CORRELATION_HEADER = "X-Correlation-ID";
export const DEFAULT_TIMEOUT_MS = 8_000;

type BackendErrorPayload = {
  code?: unknown;
  message?: unknown;
  correlationId?: unknown;
};

export type HttpResult<T> = {
  data: T | undefined;
  correlationId?: string;
};

export type RequestJsonOptions = {
  timeoutMs?: number;
  skipSessionRefresh?: boolean;
};

let refreshInFlight: Promise<void> | undefined;

export class HttpClientError extends Error {
  readonly code: string;
  readonly status?: number;
  readonly correlationId?: string;

  constructor(
    message: string,
    options: { code: string; status?: number; correlationId?: string },
  ) {
    super(message);
    this.name = "HttpClientError";
    this.code = options.code;
    this.status = options.status;
    this.correlationId = options.correlationId;
  }
}

export function resolveBackendUrl(
  runtime: "server" | "browser" = typeof window === "undefined" ? "server" : "browser",
): string {
  const configured =
    runtime === "browser"
      ? process.env.NEXT_PUBLIC_BACKEND_URL
      : process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_BACKEND_URL;
  return configured?.trim() || DEFAULT_BACKEND_URL;
}

export async function requestJson<T>(
  path: string,
  init: RequestInit = {},
  options: RequestJsonOptions = {},
): Promise<HttpResult<T>> {
  const baseUrl = resolveBackendUrl();
  const url = new URL(path, baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`);
  const headers = new Headers(init.headers);
  if (!headers.has("Accept")) headers.set("Accept", "application/json");
  if (!headers.has(CORRELATION_HEADER)) headers.set(CORRELATION_HEADER, crypto.randomUUID());

  if (init.signal?.aborted) {
    throw clientError("REQUEST_ABORTED", "La solicitud fue cancelada.");
  }

  const controller = new AbortController();
  let timeoutReached = false;
  const abortFromCaller = () => controller.abort(init.signal?.reason);
  init.signal?.addEventListener("abort", abortFromCaller, { once: true });
  const timeout = setTimeout(() => {
    timeoutReached = true;
    controller.abort();
  }, options.timeoutMs ?? DEFAULT_TIMEOUT_MS);

  let response: Response;
  try {
    response = await fetch(url, {
      ...init,
      headers,
      credentials: "include",
      signal: controller.signal,
    });
  } catch {
    if (timeoutReached) {
      throw clientError("TIMEOUT", "El servicio tardó demasiado en responder.");
    }
    if (init.signal?.aborted) {
      throw clientError("REQUEST_ABORTED", "La solicitud fue cancelada.");
    }
    throw clientError("NETWORK_ERROR", "No fue posible conectar con el servicio.");
  } finally {
    clearTimeout(timeout);
    init.signal?.removeEventListener("abort", abortFromCaller);
  }

  const correlationId = response.headers.get(CORRELATION_HEADER) ?? undefined;
  if (response.status === 401 && !options.skipSessionRefresh
      && !path.endsWith("/api/v1/session/refresh")) {
    try {
      refreshInFlight ??= refreshSession().finally(() => { refreshInFlight = undefined; });
      await refreshInFlight;
      return requestJson<T>(path, init, { ...options, skipSessionRefresh: true });
    } catch {
      // The original standardized 401 is returned below.
    }
  }
  const body = await response.text();
  if (!body.trim()) {
    if (response.ok) return { data: undefined, correlationId };
    throw clientError("HTTP_ERROR", "No fue posible completar la solicitud.", response.status, correlationId);
  }

  let payload: unknown;
  try {
    payload = JSON.parse(body);
  } catch {
    if (response.ok) {
      throw clientError(
        "INVALID_RESPONSE",
        "El servicio devolvió una respuesta no válida.",
        response.status,
        correlationId,
      );
    }
    throw clientError("HTTP_ERROR", "No fue posible completar la solicitud.", response.status, correlationId);
  }

  if (!response.ok) {
    const backendError = asBackendError(payload);
    throw clientError(
      stringValue(backendError?.code) ?? "HTTP_ERROR",
      stringValue(backendError?.message) ?? "No fue posible completar la solicitud.",
      response.status,
      correlationId ?? stringValue(backendError?.correlationId),
    );
  }

  return { data: payload as T, correlationId };
}

async function refreshSession(): Promise<void> {
  const response = await fetch(new URL("/api/v1/session/refresh", resolveBackendUrl()), {
    method: "POST", credentials: "include", headers: { [CORRELATION_HEADER]: crypto.randomUUID() },
  });
  if (response.ok) return;
  if (response.status === 409) {
    await new Promise(resolve => setTimeout(resolve, 180));
    return;
  }
  throw new Error("Session refresh failed");
}

function clientError(code: string, message: string, status?: number, correlationId?: string) {
  return new HttpClientError(message, { code, status, correlationId });
}

function asBackendError(value: unknown): BackendErrorPayload | undefined {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? (value as BackendErrorPayload)
    : undefined;
}

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}
