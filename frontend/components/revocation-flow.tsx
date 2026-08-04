"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { DigitalCredentialSelectionTransition } from "@/components/digital-credential-selection-transition";
import { RevocationReviewTransition } from "@/components/revocation-review-transition";
import { RevocationReceiptTransition } from "@/components/revocation-receipt-transition";
import { RevocationReasonTransition } from "@/components/revocation-reason-transition";
import { IdentityVerificationPanel } from "@/components/identity-verification-panel";
import type { IdentityCallbackOutcome } from "@/components/identity-callback-alert";
import { getCurrentIdentityVerification } from "@/lib/api/identity-verifications";
import { getCurrentFlowSession } from "@/lib/api/flow-session";
import { HttpClientError } from "@/lib/http-client";
import type {
    RevocationDraft,
    RevocationExecution,
    RevocationReasonCode,
} from "@/lib/api/revocation-confirmation";

type FlowView =
    | { kind: "checking" }
    | {
          kind: "identity";
          callbackOutcome?: IdentityCallbackOutcome;
          verified?: boolean;
      }
    | { kind: "selection"; selectionStale?: boolean }
    | { kind: "reason" }
    | { kind: "confirmation"; confirmed: boolean }
    | { kind: "receipt"; data?: RevocationExecution };

const emptyDraft: RevocationDraft = {
    digitalCredentialUuid: null,
    statusListIndex: null,
    reasonCode: null,
    otherReason: "",
};

export function RevocationFlow() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const [view, setView] = useState<FlowView>({ kind: "checking" });
    const [draft, setDraft] = useState<RevocationDraft>(emptyDraft);
    const [sessionDni, setSessionDni] = useState<string>();
    const redirectOutcome = asIdentityCallbackOutcome(
        searchParams.get("identityOutcome"),
    );

    const resolveCurrentView = useCallback(
        async (signal: AbortSignal) => {
            try {
                const session = await getCurrentFlowSession(signal);
                if (!session.data) throw new Error("Missing flow session");
                setSessionDni(session.data.dni);
                if (session.data.nextStep === "RECEIPT") {
                    setView({ kind: "receipt" });
                    return;
                }
                if (session.data.nextStep === "CONFIRMATION") {
                    setView({ kind: "confirmation", confirmed: true });
                    return;
                }
                if (session.data.nextStep === "DIGITAL_CREDENTIAL_SELECTION") {
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
                            result.data.nextStep === "DIGITAL_CREDENTIAL_SELECTION"
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
                <DigitalCredentialSelectionTransition
					selectionStale={view.selectionStale === true}
                    selected={draft.digitalCredentialUuid === null || draft.statusListIndex === null
                        ? null
                        : {
                            digitalCredentialUuid: draft.digitalCredentialUuid,
                            statusListIndex: draft.statusListIndex,
                        }}
                    onSelect={({ digitalCredentialUuid, statusListIndex }) =>
                        setDraft((current) => ({
                            ...current,
                            digitalCredentialUuid,
                            statusListIndex,
                        }))
                    }
                    onContinue={() => setView({ kind: "reason" })}
                />
            </div>
        );
    }

    if (view.kind === "reason") {
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <RevocationReasonTransition
                    reason={draft.reasonCode}
                    otherReason={draft.otherReason}
                    onReasonChange={(reasonCode: RevocationReasonCode) =>
                        setDraft((current) => ({ ...current, reasonCode }))
                    }
                    onOtherReasonChange={(otherReason) =>
                        setDraft((current) => ({ ...current, otherReason }))
                    }
                    onBack={() => setView({ kind: "selection" })}
                    onContinue={() =>
                        setView({ kind: "confirmation", confirmed: false })
                    }
                />
            </div>
        );
    }

    if (view.kind === "confirmation") {
        if (!sessionDni) return <FlowLoading />;
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <RevocationReviewTransition
                    dni={sessionDni}
                    draft={draft}
                    recoverConfirmed={view.confirmed}
                    onBack={() => setView({ kind: "reason" })}
					onSelectionStale={() => {
						setDraft((current) => ({
							...current,
							digitalCredentialUuid: null,
							statusListIndex: null,
						}));
						setView({ kind: "selection", selectionStale: true });
					}}
                    onCompleted={(data) => setView({ kind: "receipt", data })}
                />
            </div>
        );
    }

    if (view.kind === "receipt") {
        if (!sessionDni) return <FlowLoading />;
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <RevocationReceiptTransition
                    dni={sessionDni}
                    initialData={view.data}
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
