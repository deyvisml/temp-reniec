/** @vitest-environment happy-dom */

import { act, StrictMode, type ImgHTMLAttributes } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  IdentityCallbackAlert,
  type IdentityAlertLoader,
} from "@/components/identity-callback-alert";
import { IdentityVerificationPanel } from "@/components/identity-verification-panel";

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
  });
});
