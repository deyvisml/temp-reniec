const DEFAULT_BACKEND_URL = "http://localhost:8080";
const CORRELATION_HEADER = "X-Correlation-ID";

type BackendErrorPayload = {
  code?: unknown;
  message?: unknown;
  correlationId?: unknown;
};

export type HttpResult<T> = {
  data: T;
  correlationId?: string;
};

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

export async function requestJson<T>(path: string, init: RequestInit = {}): Promise<HttpResult<T>> {
  const baseUrl = process.env.BACKEND_URL?.trim() || DEFAULT_BACKEND_URL;
  const url = new URL(path, baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`);
  const headers = new Headers(init.headers);

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  let response: Response;

  try {
    response = await fetch(url, {
      ...init,
      headers,
      credentials: "include",
    });
  } catch {
    throw new HttpClientError("No fue posible conectar con el servicio.", {
      code: "NETWORK_ERROR",
    });
  }

  const headerCorrelationId = response.headers.get(CORRELATION_HEADER) ?? undefined;
  let payload: unknown;

  try {
    payload = await response.json();
  } catch {
    if (response.ok) {
      throw new HttpClientError("El servicio devolvió una respuesta no válida.", {
        code: "INVALID_RESPONSE",
        status: response.status,
        correlationId: headerCorrelationId,
      });
    }

    throw new HttpClientError("No fue posible completar la solicitud.", {
      code: "HTTP_ERROR",
      status: response.status,
      correlationId: headerCorrelationId,
    });
  }

  if (!response.ok) {
    const backendError = asBackendError(payload);
    const correlationId = headerCorrelationId ?? stringValue(backendError?.correlationId);

    throw new HttpClientError(
      stringValue(backendError?.message) ?? "No fue posible completar la solicitud.",
      {
        code: stringValue(backendError?.code) ?? "HTTP_ERROR",
        status: response.status,
        correlationId,
      },
    );
  }

  return {
    data: payload as T,
    correlationId: headerCorrelationId,
  };
}

function asBackendError(value: unknown): BackendErrorPayload | undefined {
  if (typeof value === "object" && value !== null && !Array.isArray(value)) {
    return value as BackendErrorPayload;
  }
  return undefined;
}

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}
