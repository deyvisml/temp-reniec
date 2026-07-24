"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { CertificateSelectionTransition } from "@/components/certificate-selection-transition";
import { IdentityVerificationPanel } from "@/components/identity-verification-panel";
import type { IdentityCallbackOutcome } from "@/components/identity-callback-alert";
import { getCurrentIdentityVerification } from "@/lib/api/identity-verifications";
import { HttpClientError } from "@/lib/http-client";

type FlowView =
  | { kind: "checking" }
  | { kind: "identity"; callbackOutcome?: IdentityCallbackOutcome; verified?: boolean }
  | { kind: "selection" };

export function CancellationFlow() {
  const router = useRouter();
  const [view, setView] = useState<FlowView>({ kind: "checking" });

  const resolveCurrentView = useCallback(async (signal: AbortSignal) => {
    try {
      const result = await getCurrentIdentityVerification(signal);
      if (!result.data) {
        setView({ kind: "identity" });
        return;
      }

      if (result.data.status === "VERIFIED") {
        setView(result.data.canContinue && result.data.nextStep === "CERTIFICATE_SELECTION"
          ? { kind: "selection" }
          : { kind: "identity" });
        return;
      }

      setView({
        kind: "identity",
        callbackOutcome: asIdentityCallbackOutcome(result.data.callbackOutcome),
      });
    } catch (error) {
      if (error instanceof HttpClientError && error.code === "REQUEST_ABORTED") return;
      if (error instanceof HttpClientError && error.status === 401) {
        router.replace("/");
        return;
      }
      setView({ kind: "identity", callbackOutcome: "ERROR" });
    }
  }, [router]);

  useEffect(() => {
    const controller = new AbortController();
    void resolveCurrentView(controller.signal);
    return () => controller.abort();
  }, [resolveCurrentView]);

  if (view.kind === "checking") return <FlowLoading />;

  if (view.kind === "identity") {
    return (
      <div className="relative overflow-hidden px-4 py-8 sm:py-12">
        <IdentityVerificationPanel
          callbackOutcome={view.callbackOutcome}
          identityVerified={view.verified}
          onContinue={() => setView({ kind: "selection" })}
        />
      </div>
    );
  }

  if (view.kind === "selection") {
    return (
      <div className="relative overflow-hidden px-4 py-8 sm:py-12">
        <CertificateSelectionTransition onBack={() => setView({ kind: "identity", verified: true })} />
      </div>
    );
  }

  return <FlowLoading />;
}

export function asIdentityCallbackOutcome(value: string | null | undefined): IdentityCallbackOutcome | undefined {
  return value === "CANCELLED" || value === "REJECTED" || value === "IDENTITY_MISMATCH"
    || value === "EXPIRED" || value === "TIMEOUT" || value === "UNAVAILABLE" || value === "ERROR"
    ? value
    : undefined;
}

function FlowLoading() {
  return (
    <section className="mx-auto grid min-h-[420px] max-w-[920px] place-items-center px-5 text-center" aria-live="polite" aria-busy="true">
      <div>
        <span className="mx-auto block size-10 animate-spin rounded-full border-4 border-[#d6e2f7] border-t-[#0755df] motion-reduce:animate-none" aria-hidden="true" />
        <p className="mt-4 font-semibold text-[#52678f]">Preparando el trámite…</p>
      </div>
    </section>
  );
}
