/** @vitest-environment happy-dom */

import { act, StrictMode, type ImgHTMLAttributes } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  IdentityCallbackAlert,
  type IdentityAlertLoader,
} from "@/components/identity-callback-alert";
import { IdentityVerificationPanel } from "@/components/identity-verification-panel";
import { RevocationFlow } from "@/components/revocation-flow";
import {
  getCurrentIdentityVerification,
  startIdentityVerification,
} from "@/lib/api/identity-verifications";
import { getCurrentFlowSession } from "@/lib/api/flow-session";
import { activeFlowRoute } from "@/lib/routes";

const navigation = vi.hoisted(() => {
  const replace = vi.fn();
  return {
    replace,
    router: { replace },
    searchParams: new URLSearchParams(),
  };
});

vi.mock("@/lib/api/identity-verifications", () => ({
  startIdentityVerification: vi.fn(),
  getCurrentIdentityVerification: vi.fn(),
}));

vi.mock("@/lib/api/flow-session", () => ({
  getCurrentFlowSession: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => navigation.router,
  useSearchParams: () => navigation.searchParams,
}));

vi.mock("next/image", () => ({
  default: ({ alt = "", priority: _priority, ...properties }: ImgHTMLAttributes<HTMLImageElement> & { priority?: boolean }) => (
    <img alt={alt} {...properties} />
  ),
}));

describe("aviso de callback de identidad en cliente", () => {
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

  it("abre exactamente un modal bajo Strict Mode", async () => {
    const fire = vi.fn().mockResolvedValue({ isConfirmed: true });
    const close = vi.fn();
    const loadAlert = vi.fn().mockResolvedValue({ default: { fire, close } }) as IdentityAlertLoader;
    const onAcknowledge = vi.fn();

    await act(async () => {
      root.render(
        <StrictMode>
          <IdentityCallbackAlert
            outcome="UNAVAILABLE"
            onAcknowledge={onAcknowledge}
            loadAlert={loadAlert}
          />
        </StrictMode>,
      );
      await new Promise((resolve) => setTimeout(resolve, 20));
    });

    expect(loadAlert).toHaveBeenCalledTimes(2);
    expect(fire).toHaveBeenCalledTimes(1);
    expect(onAcknowledge).toHaveBeenCalledTimes(1);
  });

  it("reconoce el resultado aunque el modal no pueda cargarse", async () => {
    const loadAlert = vi.fn().mockRejectedValue(new Error("chunk unavailable")) as IdentityAlertLoader;
    const onAcknowledge = vi.fn();

    await act(async () => {
      root.render(
        <StrictMode>
          <IdentityCallbackAlert
            outcome="UNAVAILABLE"
            onAcknowledge={onAcknowledge}
            loadAlert={loadAlert}
          />
        </StrictMode>,
      );
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(onAcknowledge).toHaveBeenCalledTimes(1);
  });

  it("mantiene el aviso fijo después de reconocer y cerrar el modal", async () => {
    const onCallbackOutcomeAcknowledged = vi.fn();
    const loadAlert = vi.fn().mockResolvedValue({
      default: { fire: vi.fn().mockResolvedValue({ isConfirmed: true }), close: vi.fn() },
    }) as IdentityAlertLoader;

    await act(async () => {
      root.render(
        <StrictMode>
          <IdentityVerificationPanel
            callbackOutcome="UNAVAILABLE"
            identityAlertLoader={loadAlert}
            onCallbackOutcomeAcknowledged={onCallbackOutcomeAcknowledged}
          />
        </StrictMode>,
      );
      await new Promise((resolve) => setTimeout(resolve, 20));
    });

    expect(onCallbackOutcomeAcknowledged).toHaveBeenCalledTimes(1);
    expect(container.querySelectorAll('[role="alert"]')).toHaveLength(1);
    expect(container.textContent).toContain("ID Perú no está disponible");
    expect(container.textContent).toContain("Reintentar verificación");

    await act(async () => window.dispatchEvent(new Event("pageshow")));

    expect(container.querySelectorAll('[role="alert"]')).toHaveLength(1);
    expect(container.textContent).toContain("ID Perú no está disponible");
  });

  it("bloquea dobles inicios mientras prepara la salida hacia ID Perú", async () => {
    vi.mocked(startIdentityVerification).mockReturnValue(new Promise<never>(() => undefined));

    await act(async () => root.render(<IdentityVerificationPanel />));
    const button = Array.from(container.querySelectorAll("button"))
      .find((candidate) => candidate.textContent?.includes("Verificar identidad"));
    expect(button).not.toBeNull();

    await act(async () => {
      button?.click();
      button?.click();
    });

    expect(startIdentityVerification).toHaveBeenCalledTimes(1);
    expect(button?.disabled).toBe(true);
    expect(button?.textContent).toContain("Preparando verificación…");
    expect(container.querySelector("section")?.getAttribute("aria-busy")).toBe("true");
  });

  it("limpia una cancelación histórica sin mostrar error y permite verificar nuevamente", async () => {
    navigation.searchParams = new URLSearchParams("identityOutcome=CANCELLED");
    vi.mocked(getCurrentFlowSession).mockResolvedValue({
      data: { dni: "00000001", nextStep: "IDENTITY_VERIFICATION" },
    } as Awaited<ReturnType<typeof getCurrentFlowSession>>);
    vi.mocked(getCurrentIdentityVerification).mockResolvedValue({
      data: {
        status: "CANCELLED",
        canContinue: false,
        nextStep: "IDENTITY_VERIFICATION",
        callbackOutcome: null,
      },
    } as Awaited<ReturnType<typeof getCurrentIdentityVerification>>);

    await act(async () => {
      root.render(<RevocationFlow />);
      await new Promise((resolve) => setTimeout(resolve, 20));
    });

    expect(navigation.replace).toHaveBeenCalledWith(activeFlowRoute(), { scroll: false });
    expect(container.querySelector('[role="alert"]')).toBeNull();
    expect(container.textContent).not.toContain("Verificación revocada");
    expect(container.textContent).not.toContain("No pudimos verificar tu identidad");
    expect(container.textContent).toContain("Verificar identidad");
  });
});
