/** @vitest-environment happy-dom */

import { act, StrictMode, type ImgHTMLAttributes } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { RevocationFlow } from "@/components/revocation-flow";
import {
  getCurrentIdentityVerification,
  startIdentityVerification,
} from "@/lib/api/identity-verifications";
import { getCurrentFlowSession } from "@/lib/api/flow-session";
import { HttpClientError } from "@/lib/http-client";

const navigation = vi.hoisted(() => {
  const replace = vi.fn();
  return {
    replace,
    router: { replace },
    searchParams: new URLSearchParams(),
  };
});

vi.mock("next/navigation", () => ({
  useRouter: () => navigation.router,
  useSearchParams: () => navigation.searchParams,
}));

vi.mock("next/image", () => ({
  default: ({ alt = "", priority: _priority, ...properties }: ImgHTMLAttributes<HTMLImageElement> & { priority?: boolean }) => (
    <img alt={alt} {...properties} />
  ),
}));

vi.mock("@/lib/api/identity-verifications", () => ({
  startIdentityVerification: vi.fn(),
  getCurrentIdentityVerification: vi.fn(),
}));

vi.mock("@/lib/api/flow-session", () => ({
  getCurrentFlowSession: vi.fn(),
}));

vi.mock("@/components/digital-credential-selection-transition", () => ({
  DigitalCredentialSelectionTransition: () => (
    <section data-testid="digital-credential-selection">Paso 2: Selección</section>
  ),
}));

describe("recuperación del flujo desde el historial", () => {
  let container: HTMLDivElement;
  let root: Root;

  beforeEach(() => {
    (globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
    vi.mocked(startIdentityVerification).mockReset();
    vi.mocked(getCurrentIdentityVerification).mockReset();
    vi.mocked(getCurrentFlowSession).mockReset();
    navigation.replace.mockReset();
    navigation.searchParams = new URLSearchParams();
    container = document.createElement("div");
    document.body.append(container);
    root = createRoot(container);
  });

  afterEach(async () => {
    await act(async () => root.unmount());
    container.remove();
    delete (globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT;
  });

  it("resuelve el paso correctamente bajo el doble montaje de Strict Mode", async () => {
    vi.mocked(getCurrentFlowSession).mockResolvedValue(identitySession());
    vi.mocked(getCurrentIdentityVerification).mockResolvedValue(startedIdentity());

    await act(async () => {
      root.render(
        <StrictMode>
          <RevocationFlow />
        </StrictMode>,
      );
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(identityButton(container).disabled).toBe(false);
    expect(container.querySelector('[role="alert"]')).toBeNull();
  });

  it("abre directamente selección cuando la sesión verificada no tiene credenciales vigentes", async () => {
    vi.mocked(getCurrentFlowSession).mockResolvedValue(selectionSession());

    await renderFlow(root);

    expect(container.querySelector('[data-testid="digital-credential-selection"]')).not.toBeNull();
    expect(container.textContent).not.toContain("Verificar identidad");
    expect(getCurrentIdentityVerification).not.toHaveBeenCalled();
    expect(startIdentityVerification).not.toHaveBeenCalled();
  });

  it("restaura el paso pendiente y desbloquea un inicio abandonado", async () => {
    vi.mocked(getCurrentFlowSession).mockResolvedValue(identitySession());
    vi.mocked(getCurrentIdentityVerification).mockResolvedValue(startedIdentity());
    vi.mocked(startIdentityVerification).mockReturnValue(new Promise<never>(() => undefined));

    await renderFlow(root);
    const initialButton = identityButton(container);
    await act(async () => initialButton.click());
    expect(initialButton.disabled).toBe(true);

    await dispatchPageShow(true);

    expect(getCurrentFlowSession).toHaveBeenCalledTimes(2);
    expect(container.textContent).toContain("Verificar identidad");
    const restoredButton = identityButton(container);
    expect(restoredButton).not.toBe(initialButton);
    expect(restoredButton.disabled).toBe(false);
    expect(startIdentityVerification).toHaveBeenCalledTimes(1);
    expect(getCurrentIdentityVerification).toHaveBeenCalledTimes(2);
  });

  it("oculta el paso antiguo y recupera selección cuando la identidad ya fue verificada", async () => {
    const restoredSession = deferred<Awaited<ReturnType<typeof getCurrentFlowSession>>>();
    vi.mocked(getCurrentFlowSession)
      .mockResolvedValueOnce(identitySession())
      .mockReturnValueOnce(restoredSession.promise);
    vi.mocked(getCurrentIdentityVerification).mockResolvedValue(startedIdentity());

    await renderFlow(root);
    expect(container.textContent).toContain("Verificar identidad");

    await act(async () => window.dispatchEvent(pageShowEvent(true)));

    expect(container.textContent).toContain("Preparando el trámite");
    expect(container.textContent).not.toContain("Verificar identidad");
    expect(container.querySelector('[role="alert"]')).toBeNull();

    await act(async () => {
      restoredSession.resolve(selectionSession());
      await Promise.resolve();
    });

    expect(container.querySelector('[data-testid="digital-credential-selection"]')).not.toBeNull();
    expect(container.textContent).not.toContain("Verificar identidad");
    expect(getCurrentIdentityVerification).toHaveBeenCalledTimes(1);
    expect(startIdentityVerification).not.toHaveBeenCalled();
  });

  it("muestra recuperación neutral y permite reintentar sin ofrecer ID Perú", async () => {
    vi.mocked(getCurrentFlowSession)
      .mockRejectedValueOnce(new HttpClientError("sin conexión", { code: "NETWORK_ERROR" }))
      .mockResolvedValueOnce(selectionSession());

    await renderFlow(root);

    expect(container.textContent).toContain("No pudimos recuperar el trámite");
    expect(container.textContent).not.toContain("Verificar identidad");
    expect(container.querySelector('[role="alert"]')).toBeNull();

    const retry = Array.from(container.querySelectorAll("button"))
      .find((button) => button.textContent === "Reintentar");
    expect(retry).not.toBeUndefined();
    await act(async () => retry?.click());
    await act(async () => Promise.resolve());

    expect(container.querySelector('[data-testid="digital-credential-selection"]')).not.toBeNull();
    expect(getCurrentIdentityVerification).not.toHaveBeenCalled();
  });

  it("resincroniza sin modal si el estado cambia al iniciar ID Perú", async () => {
    vi.mocked(getCurrentFlowSession)
      .mockResolvedValueOnce(identitySession())
      .mockResolvedValueOnce(selectionSession());
    vi.mocked(getCurrentIdentityVerification).mockResolvedValue(startedIdentity());
    vi.mocked(startIdentityVerification).mockRejectedValue(
      new HttpClientError("el paso ya fue superado", {
        code: "IDENTITY_CONTINUITY_REQUIRED",
        status: 401,
      }),
    );

    await renderFlow(root);
    await act(async () => {
      identityButton(container).click();
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(container.querySelector('[data-testid="digital-credential-selection"]')).not.toBeNull();
    expect(container.querySelector('[role="alert"]')).toBeNull();
    expect(getCurrentFlowSession).toHaveBeenCalledTimes(2);
    expect(startIdentityVerification).toHaveBeenCalledTimes(1);
  });

  it("redirige al inicio si la sesión expiró al resincronizar una vista antigua", async () => {
    vi.mocked(getCurrentFlowSession)
      .mockResolvedValueOnce(identitySession())
      .mockRejectedValueOnce(
        new HttpClientError("sesión expirada", {
          code: "SESSION_EXPIRED",
          status: 401,
        }),
      );
    vi.mocked(getCurrentIdentityVerification).mockResolvedValue(startedIdentity());
    vi.mocked(startIdentityVerification).mockRejectedValue(
      new HttpClientError("el paso ya fue superado", {
        code: "IDENTITY_CONTINUITY_REQUIRED",
        status: 401,
      }),
    );

    await renderFlow(root);
    await act(async () => {
      identityButton(container).click();
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(navigation.replace).toHaveBeenCalledWith("/");
    expect(container.querySelector('[role="alert"]')).toBeNull();
    expect(getCurrentFlowSession).toHaveBeenCalledTimes(2);
    expect(startIdentityVerification).toHaveBeenCalledTimes(1);
  });

  it("redirige al inicio cuando la sesión ya no es válida", async () => {
    vi.mocked(getCurrentFlowSession).mockRejectedValue(
      new HttpClientError("sesión expirada", { code: "SESSION_EXPIRED", status: 401 }),
    );

    await renderFlow(root);

    expect(navigation.replace).toHaveBeenCalledWith("/");
    expect(container.textContent).not.toContain("Verificar identidad");
    expect(getCurrentIdentityVerification).not.toHaveBeenCalled();
  });
});

function identitySession() {
  return {
    data: { dni: "00000001", nextStep: "IDENTITY_VERIFICATION" },
  } as Awaited<ReturnType<typeof getCurrentFlowSession>>;
}

function selectionSession() {
  return {
    data: {
      dni: "00000001",
      requestStatus: "NO_DIGITAL_CREDENTIALS_AVAILABLE",
      sessionStatus: "IDENTITY_VERIFIED",
      nextStep: "DIGITAL_CREDENTIAL_SELECTION",
    },
  } as Awaited<ReturnType<typeof getCurrentFlowSession>>;
}

function startedIdentity() {
  return {
    data: {
      status: "STARTED",
      canContinue: false,
      nextStep: "IDENTITY_VERIFICATION",
      callbackOutcome: null,
    },
  } as Awaited<ReturnType<typeof getCurrentIdentityVerification>>;
}

async function renderFlow(root: Root) {
  await act(async () => {
    root.render(<RevocationFlow />);
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
}

async function dispatchPageShow(persisted: boolean) {
  await act(async () => {
    window.dispatchEvent(pageShowEvent(persisted));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
}

function pageShowEvent(persisted: boolean): PageTransitionEvent {
  const event = new Event("pageshow") as PageTransitionEvent;
  Object.defineProperty(event, "persisted", { value: persisted });
  return event;
}

function identityButton(container: HTMLElement): HTMLButtonElement {
  const button = Array.from(container.querySelectorAll("button"))
    .find((candidate) => candidate.textContent?.includes("Verificar identidad"));
  if (!button) throw new Error("No se encontró el botón de identidad");
  return button;
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((complete) => {
    resolve = complete;
  });
  return { promise, resolve };
}
