"use client";

import { useCallback, useEffect, useRef, useState } from "react";
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
import { activeFlowRoute } from "@/lib/routes";
import type {
    RevocationDraft,
    RevocationExecution,
    RevocationReasonCode,
} from "@/lib/api/revocation-confirmation";

type FlowView =
    | { kind: "checking" }
    | { kind: "recovery" }
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
    const rawRedirectOutcome = searchParams.get("identityOutcome");
    const flowRoute = activeFlowRoute();
    const [view, setView] = useState<FlowView>({ kind: "checking" });
    const [identityViewVersion, setIdentityViewVersion] = useState(0);
    const [draft, setDraft] = useState<RevocationDraft>(emptyDraft);
    const [sessionDni, setSessionDni] = useState<string>();
    const activeResolutionController = useRef<AbortController | null>(null);
    const redirectOutcome = asIdentityCallbackOutcome(
        rawRedirectOutcome,
    );
    const legacyCancellation = rawRedirectOutcome?.toUpperCase() === "CANCELLED";

    useEffect(() => {
        if (legacyCancellation) router.replace(flowRoute, { scroll: false });
    }, [flowRoute, legacyCancellation, router]);

    const applyAuthorizedSession = useCallback(
        (session: Awaited<ReturnType<typeof getCurrentFlowSession>>["data"]) => {
            setSessionDni(session.dni);
            if (session.nextStep === "RECEIPT") {
                setView({ kind: "receipt" });
                return false;
            }
            if (session.nextStep === "CONFIRMATION") {
                setView({ kind: "confirmation", confirmed: true });
                return false;
            }
            if (session.nextStep === "DIGITAL_CREDENTIAL_SELECTION") {
                setView({ kind: "selection" });
                return false;
            }
            return true;
        },
        [],
    );

    const resolveCurrentView = useCallback(
        async (signal: AbortSignal): Promise<void> => {
            let session: Awaited<ReturnType<typeof getCurrentFlowSession>>;
            try {
                session = await getCurrentFlowSession(signal);
            } catch (error) {
                if (isAbortedRequest(error)) return;
                if (error instanceof HttpClientError && error.status === 401) {
                    router.replace("/");
                    return;
                }
                setView({ kind: "recovery" });
                return;
            }

            const identityAuthorized = applyAuthorizedSession(session.data);
            if (!identityAuthorized) return;

            try {
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
                if (isAbortedRequest(error)) return;
                if (error instanceof HttpClientError && error.status === 401) {
                    router.replace("/");
                    return;
                }
                setView({ kind: "identity", callbackOutcome: "ERROR" });
            }
        },
        [applyAuthorizedSession, redirectOutcome, router],
    );

    const cancelActiveResolution = useCallback(() => {
        activeResolutionController.current?.abort();
        activeResolutionController.current = null;
    }, []);

    const runResolution = useCallback(
        () => {
            cancelActiveResolution();
            setView({ kind: "checking" });
            setIdentityViewVersion((current) => current + 1);
            const controller = new AbortController();
            activeResolutionController.current = controller;
            void resolveCurrentView(controller.signal).finally(() => {
                if (activeResolutionController.current === controller) {
                    activeResolutionController.current = null;
                }
            });
        },
        [cancelActiveResolution, resolveCurrentView],
    );

    useEffect(() => {
        void runResolution();
        return cancelActiveResolution;
    }, [cancelActiveResolution, runResolution]);

    useEffect(() => {
        function restoreAuthoritativeView(event: PageTransitionEvent) {
            if (!event.persisted) return;
            void runResolution();
        }

        window.addEventListener("pageshow", restoreAuthoritativeView);
        return () => {
            window.removeEventListener("pageshow", restoreAuthoritativeView);
        };
    }, [runResolution]);

    if (view.kind === "checking") return <FlowLoading />;
    if (view.kind === "recovery") {
        return <FlowRecovery onRetry={() => void runResolution()} />;
    }

    if (view.kind === "identity") {
        return (
            <div className="relative px-4 py-8 sm:py-12 overflow-hidden">
                <IdentityVerificationPanel
                    key={identityViewVersion}
                    callbackOutcome={view.callbackOutcome}
                    identityVerified={view.verified}
                    onContinue={() => setView({ kind: "selection" })}
                    onSessionStale={() => void runResolution()}
                    onCallbackOutcomeAcknowledged={() => {
                        if (redirectOutcome)
                            router.replace(flowRoute, { scroll: false });
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
    return value === "REJECTED" ||
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

function FlowRecovery({ onRetry }: { onRetry: () => void }) {
    return (
        <section
            className="place-items-center grid mx-auto px-5 max-w-[920px] min-h-[420px] text-center"
            aria-labelledby="flow-recovery-title"
        >
            <div className="max-w-[520px]">
                <h1
                    id="flow-recovery-title"
                    className="text-balance text-2xl font-black text-[#061a50]"
                >
                    No pudimos recuperar el trámite
                </h1>
                <p className="mt-3 text-pretty leading-7 text-[#52678f]">
                    Comprueba tu conexión y vuelve a consultar el paso actual.
                </p>
                <button
                    type="button"
                    onClick={onRetry}
                    className="mt-6 min-h-11 cursor-pointer rounded-lg bg-reniec-red px-6 font-bold text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df]"
                >
                    Reintentar
                </button>
            </div>
        </section>
    );
}

function isAbortedRequest(error: unknown): boolean {
    return error instanceof HttpClientError && error.code === "REQUEST_ABORTED";
}
