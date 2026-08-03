"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import {
    OutcomeView,
    ProcessingView,
    ReviewView,
    SubmissionUncertainView,
} from "@/components/revocation-review-views";
import { RevocationStepper } from "@/components/revocation-stepper";
import {
    confirmCurrentRevocation,
    getConfirmedRevocationReview,
    getCurrentRevocationOutcome,
    previewCurrentRevocation,
    resumeCurrentRevocationExecution,
    retryCurrentRevocationReceipt,
    type RevocationDraft,
    type CompleteRevocationDraft,
    type RevocationExecution,
    type RevocationReview,
} from "@/lib/api/revocation-confirmation";
import { HttpClientError } from "@/lib/http-client";

export { ReviewView } from "@/components/revocation-review-views";

type State =
    | { kind: "loading" }
    | {
          kind: "ready";
          review: RevocationReview;
          outcome?: RevocationExecution;
      }
    | { kind: "error"; title: string; description: string; reload: boolean };

export function RevocationReviewTransition({
    dni,
    draft,
    recoverConfirmed,
    onBack,
    onCompleted,
}: {
    dni: string;
    draft: RevocationDraft;
    recoverConfirmed: boolean;
    onBack: () => void;
    onCompleted: (data: RevocationExecution) => void;
}) {
    const [state, setState] = useState<State>({ kind: "loading" });
    const [accepted, setAccepted] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [submissionUncertain, setSubmissionUncertain] = useState(false);
    const submissionInFlight = useRef(false);

    const load = useCallback(
        async (signal?: AbortSignal) => {
            setState({ kind: "loading" });
            try {
                const complete = completeDraft(draft);
                if (!recoverConfirmed && !complete) {
                    onBack();
                    return;
                }
                const reviewResult = recoverConfirmed
                    ? await getConfirmedRevocationReview()
                    : await previewCurrentRevocation(complete!);
                if (!reviewResult.data) throw new Error("Missing review");
                let outcome: RevocationExecution | undefined;
                if (recoverConfirmed) {
                    const result = await getCurrentRevocationOutcome();
                    outcome = result.data;
                    if (
                        outcome?.state === "PROCESSING" &&
                        (outcome.requestStatus === "CONFIRMED" ||
                            outcome.requestStatus === "REVOCATION_IN_PROGRESS")
                    ) {
                        outcome = (await resumeCurrentRevocationExecution())
                            .data;
                    }
                    if (outcome?.state === "SUCCEEDED") {
                        onCompleted(outcome);
                        return;
                    }
                }
                if (!signal?.aborted)
                    setState({
                        kind: "ready",
                        review: reviewResult.data,
                        outcome,
                    });
            } catch (error) {
                if (
                    signal?.aborted ||
                    (error instanceof HttpClientError &&
                        error.code === "REQUEST_ABORTED")
                )
                    return;
                if (
                    error instanceof HttpClientError &&
                    (error.status === 401 || error.code.startsWith("SESSION_"))
                ) {
                    window.location.assign("/");
                    return;
                }
                setState(errorState(error));
            }
        },
        [draft, onBack, onCompleted, recoverConfirmed],
    );

    useEffect(() => {
        const controller = new AbortController();
        void load(controller.signal);
        return () => controller.abort();
    }, [load]);

    useEffect(() => {
        if (
            state.kind !== "ready" ||
            state.outcome?.state !== "PROCESSING" ||
            submissionUncertain
        ) return;

        let disposed = false;
        const poll = async () => {
            try {
                const result = await getCurrentRevocationOutcome();
                if (disposed || !result.data) return;
                if (result.data.state === "SUCCEEDED") {
                    onCompleted(result.data);
                    return;
                }
                setState((current) => current.kind === "ready"
                    ? { ...current, outcome: result.data }
                    : current);
            } catch (error) {
                if (disposed) return;
                if (error instanceof HttpClientError
                    && (error.status === 401 || error.code.startsWith("SESSION_"))) {
                    window.location.assign("/");
                }
            }
        };

        const timer = window.setInterval(() => void poll(), 5_000);
        return () => {
            disposed = true;
            window.clearInterval(timer);
        };
    }, [onCompleted, state, submissionUncertain]);

    const run = async (
        action: () => Promise<{ data: RevocationExecution | undefined }>,
        uncertainOnTransportFailure = false,
    ) => {
        if (submissionInFlight.current) return;
        submissionInFlight.current = true;
        setSubmitting(true);
        setSubmissionUncertain(false);
        try {
            const result = await action();
            if (!result.data) throw new Error("Missing outcome");
            if (result.data.state === "SUCCEEDED") {
                onCompleted(result.data);
                return;
            }
            setState((current) =>
                current.kind === "ready"
                    ? { ...current, outcome: result.data }
                    : current,
            );
        } catch (error) {
            if (
                error instanceof HttpClientError &&
                (error.status === 401 || error.code.startsWith("SESSION_"))
            ) {
                window.location.assign("/");
                return;
            }
            if (
                uncertainOnTransportFailure &&
                error instanceof HttpClientError &&
                (error.code === "NETWORK_ERROR" || error.code === "TIMEOUT")
            ) {
                setSubmissionUncertain(true);
                return;
            }
            setState(errorState(error));
        } finally {
            submissionInFlight.current = false;
            setSubmitting(false);
        }
    };

    const submit = () =>
        run(async () => {
            if (state.kind !== "ready" || !accepted)
                throw new Error("Consent required");
            const complete = completeDraft(draft);
            if (!complete) throw new Error("Missing draft");
            return confirmCurrentRevocation(
                complete,
                state.review.consentVersion,
            );
        }, true);

    return (
        <section
            className="mx-auto w-full max-w-[1040px]"
            aria-labelledby="review-title"
        >
            <div className="px-2 sm:px-8 lg:px-14">
                <RevocationStepper
                    currentStep={4}
                    navigableSteps={
                        state.kind === "ready" &&
                        !state.outcome &&
                        !submissionUncertain &&
                        !submitting
                            ? [3]
                            : []
                    }
                    onNavigate={(step) => {
                        if (step === 3) onBack();
                    }}
                />
            </div>
            <div className="bg-white mx-2 sm:mx-8 lg:mx-14 mt-6 px-4 sm:px-8 py-7 sm:py-9 rounded-2xl">
                {state.kind === "loading" ? <LoadingState /> : null}
                {state.kind === "error" ? (
                    <ErrorState state={state} onRetry={() => void load()} />
                ) : null}
                {state.kind === "ready" && submissionUncertain ? (
                    <SubmissionUncertainView
                        submitting={submitting}
                        onRetry={() => void submit()}
                    />
                ) : null}
                {state.kind === "ready" &&
                !submissionUncertain &&
                state.outcome?.state === "PROCESSING" ? (
                    <ProcessingView outcome={state.outcome} />
                ) : null}
                {state.kind === "ready" &&
                !submissionUncertain &&
                state.outcome?.state !== "PROCESSING" &&
                state.outcome ? (
                    <OutcomeView
                        outcome={state.outcome}
                        submitting={submitting}
                        onRefresh={() =>
                            void run(resumeCurrentRevocationExecution)
                        }
                        onRetryReceipt={() =>
                            void run(retryCurrentRevocationReceipt)
                        }
                    />
                ) : null}
                {state.kind === "ready" &&
                !submissionUncertain &&
                !state.outcome ? (
                    <ReviewView
                        dni={dni}
                        review={state.review}
                        accepted={accepted}
                        submitting={submitting}
                        onAccepted={setAccepted}
                        onBack={onBack}
                        onConfirm={() => void submit()}
                    />
                ) : null}
            </div>
        </section>
    );
}

function LoadingState() {
    return (
        <div
            className="place-items-center grid min-h-[420px] text-center"
            aria-live="polite"
            aria-busy="true"
        >
            <div>
                <span className="block mx-auto border-[#d6e2f7] border-4 border-t-reniec-red rounded-full size-10 animate-spin motion-reduce:animate-none" />
                <p className="mt-4 font-semibold text-[#52678f]">
                    Preparando la confirmación…
                </p>
            </div>
        </div>
    );
}
function ErrorState({
    state,
    onRetry,
}: {
    state: Extract<State, { kind: "error" }>;
    onRetry: () => void;
}) {
    return (
        <div
            className="place-items-center grid min-h-[390px] text-center"
            role="alert"
        >
            <div className="max-w-[520px]">
                <h1
                    id="review-title"
                    className="font-black text-[#061a50] text-2xl"
                >
                    {state.title}
                </h1>
                <p className="mt-3 text-[#52678f] leading-7">
                    {state.description}
                </p>
                <button
                    type="button"
                    onClick={
                        state.reload ? () => window.location.reload() : onRetry
                    }
                    className="bg-reniec-red mt-6 px-6 py-3 rounded-lg font-bold text-white"
                >
                    {state.reload ? "Recargar" : "Reintentar"}
                </button>
            </div>
        </div>
    );
}
function errorState(error: unknown): Extract<State, { kind: "error" }> {
    if (error instanceof HttpClientError) {
        if (error.status === 409 || error.code === "CONSENT_VERSION_CHANGED") {
            return {
                kind: "error",
                title: "La información fue actualizada",
                description:
                    "Recarga el resumen y revisa nuevamente la confirmación.",
                reload: true,
            };
        }
        if (error.status === 403 || error.status === 422) {
            return {
                kind: "error",
                title: "Este paso ya no está disponible",
                description:
                    "Recarga la página para recuperar el estado vigente de la solicitud.",
                reload: true,
            };
        }
        if (error.status === 503 || error.code === "REVOCATION_UNAVAILABLE") {
            return {
                kind: "error",
                title: "El servicio de revocación no está disponible",
                description:
                    "Tu decisión no fue registrada. Inténtalo nuevamente más tarde.",
                reload: false,
            };
        }
    }
    return {
        kind: "error",
        title: "No pudimos completar la operación",
        description:
            "Inténtalo nuevamente. Si el problema continúa, vuelve a iniciar la solicitud.",
        reload: false,
    };
}
function completeDraft(draft: RevocationDraft): CompleteRevocationDraft | null {
    if (!draft.digitalCredentialUuid || !draft.reasonCode) return null;
    return {
        digitalCredentialUuid: draft.digitalCredentialUuid,
        reasonCode: draft.reasonCode,
        ...(draft.reasonCode === "OTHER"
            ? { otherReason: draft.otherReason.trim() }
            : {}),
    };
}
