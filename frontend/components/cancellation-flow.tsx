"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { CertificateSelectionTransition } from "@/components/certificate-selection-transition";
import { CancellationReviewTransition } from "@/components/cancellation-review-transition";
import { CancellationReasonTransition } from "@/components/cancellation-reason-transition";
import { IdentityVerificationPanel } from "@/components/identity-verification-panel";
import type { IdentityCallbackOutcome } from "@/components/identity-callback-alert";
import { getCurrentIdentityVerification } from "@/lib/api/identity-verifications";
import { getCurrentFlowSession } from "@/lib/api/flow-session";
import { HttpClientError } from "@/lib/http-client";

type FlowView =
    | { kind: "checking" }
    | {
          kind: "identity";
          callbackOutcome?: IdentityCallbackOutcome;
          verified?: boolean;
      }
    | { kind: "selection" }
    | { kind: "reason" }
    | { kind: "confirmation" };

export function CancellationFlow() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const [view, setView] = useState<FlowView>({ kind: "checking" });
    const redirectOutcome = asIdentityCallbackOutcome(
        searchParams.get("identityOutcome"),
    );

    const resolveCurrentView = useCallback(
        async (signal: AbortSignal) => {
            try {
                const session = await getCurrentFlowSession(signal);
                if (!session.data) throw new Error("Missing flow session");
                if (session.data.nextStep === "CONFIRMATION") {
                    setView({ kind: "confirmation" });
                    return;
                }
                if (session.data.nextStep === "REASON") {
                    setView({ kind: "reason" });
                    return;
                }
                if (session.data.nextStep === "CERTIFICATE_SELECTION") {
                    setView({ kind: "selection" });
                    return;
                }
                const result = await getCurrentIdentityVerification(signal);
                if (!result.data) {
                    setView({ kind: "identity" });
                    return;
                }

                if (result.data.status === "VERIFIED") {
                    setView(
                        result.data.canContinue &&
                            result.data.nextStep === "CERTIFICATE_SELECTION"
                            ? { kind: "selection" }
                            : { kind: "identity" },
                    );
                    return;
                }

                setView({
                    kind: "identity",
                    callbackOutcome:
                        redirectOutcome ??
                        asIdentityCallbackOutcome(result.data.callbackOutcome),
                });
            } catch (error) {
                if (
                    error instanceof HttpClientError &&
                    error.code === "REQUEST_ABORTED"
                )
                    return;
                if (error instanceof HttpClientError && error.status === 401) {
                    router.replace("/");
                    return;
                }
                setView({ kind: "identity", callbackOutcome: "ERROR" });
            }
        },
        [redirectOutcome, router],
    );

    useEffect(() => {
        const controller = new AbortController();
        void resolveCurrentView(controller.signal);
        return () => controller.abort();
    }, [resolveCurrentView]);

    if (view.kind === "checking") return <FlowLoading />;

    if (view.kind === "identity") {
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <IdentityVerificationPanel
                    callbackOutcome={view.callbackOutcome}
                    identityVerified={view.verified}
                    onContinue={() => setView({ kind: "selection" })}
                    onCallbackOutcomeAcknowledged={() => {
                        if (redirectOutcome)
                            router.replace("/autorizacion", { scroll: false });
                    }}
                />
            </div>
        );
    }

    if (view.kind === "selection") {
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <CertificateSelectionTransition
                    onContinue={() => setView({ kind: "reason" })}
                />
            </div>
        );
    }

    if (view.kind === "reason") {
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <CancellationReasonTransition
                    onBack={() => setView({ kind: "selection" })}
                    onContinue={() => setView({ kind: "confirmation" })}
                />
            </div>
        );
    }

    if (view.kind === "confirmation") {
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <CancellationReviewTransition
                    onBack={() => setView({ kind: "reason" })}
                />
            </div>
        );
    }

    return <FlowLoading />;
}

export function asIdentityCallbackOutcome(
    value: string | null | undefined,
): IdentityCallbackOutcome | undefined {
    return value === "CANCELLED" ||
        value === "REJECTED" ||
        value === "IDENTITY_MISMATCH" ||
        value === "EXPIRED" ||
        value === "TIMEOUT" ||
        value === "UNAVAILABLE" ||
        value === "ERROR"
        ? value
        : undefined;
}

function FlowLoading() {
    return (
        <section
            className="place-items-center grid mx-auto px-5 max-w-[920px] min-h-[420px] text-center"
            aria-live="polite"
            aria-busy="true"
        >
            <div>
                <span
                    className="block mx-auto border-[#d6e2f7] border-4 border-t-[#0755df] rounded-full size-10 animate-spin motion-reduce:animate-none"
                    aria-hidden="true"
                />
                <p className="mt-4 font-semibold text-[#52678f]">
                    Preparando el trámite…
                </p>
            </div>
        </section>
    );
}
