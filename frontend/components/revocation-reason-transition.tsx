"use client";

import { RevocationStepper } from "@/components/revocation-stepper";
import { FlowNavigationButton } from "@/components/flow-navigation-button";
import { FlowStepContent } from "@/components/flow-step-content";
import type { RevocationReasonCode } from "@/lib/api/revocation-confirmation";

const options: ReadonlyArray<{ code: RevocationReasonCode; title: string; description: string }> = [
  { code: "THEFT", title: "Robo", description: "Tu dispositivo o medio de almacenamiento fue robado." },
  { code: "LOSS", title: "Pérdida", description: "Has perdido tu dispositivo o medio de almacenamiento." },
  { code: "DEVICE_OR_NUMBER_CHANGE", title: "Cambio de equipo o número", description: "Ya no usarás el equipo o número asociado." },
  { code: "SUSPECTED_UNAUTHORIZED_USE", title: "Sospecha de uso no autorizado", description: "Sospechas que alguien más pudo haber accedido." },
  { code: "OTHER", title: "Otro motivo", description: "Ninguna de las opciones anteriores describe mi caso." },
];

export function RevocationReasonTransition({
  reason,
  otherReason,
  onReasonChange,
  onOtherReasonChange,
  onBack,
  onContinue,
}: {
  reason: RevocationReasonCode | null;
  otherReason: string;
  onReasonChange: (reason: RevocationReasonCode) => void;
  onOtherReasonChange: (description: string) => void;
  onBack: () => void;
  onContinue: () => void;
}) {
  const descriptionIsValid = reason !== "OTHER"
    || (otherReason.trim().length >= 10 && otherReason.trim().length <= 300);
  const canContinue = reason !== null && descriptionIsValid;

  return (
    <section className="mx-auto w-full max-w-[1040px]" aria-labelledby="reason-title">
      <div className="px-2 sm:px-8 lg:px-14">
        <RevocationStepper currentStep={3} navigableSteps={[2]} onNavigate={step => step === 2 && onBack()} />
      </div>
      <div className="mx-2 mt-6 rounded-[22px] border border-[#dfe7f3] bg-white/95 px-4 py-7 sm:mx-8 sm:px-8 sm:py-9 lg:mx-14">
        <FlowStepContent>
          <header className="text-center">
            <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 3 DE 5</p>
            <h1 id="reason-title" className="mt-4 text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">¿Cuál es el motivo de la revocación?</h1>
            <p className="mt-3 text-sm leading-6 text-[#52678f] sm:text-base">Selecciona la causa que mejor describa tu situación.</p>
          </header>

          <fieldset className="mt-7 space-y-3">
            <legend className="sr-only">Motivo de revocación</legend>
            {options.map(option => {
              const selected = reason === option.code;
              return (
                <label key={option.code} className={`block cursor-pointer rounded-xl border px-4 py-4 transition-[border-color,background-color] duration-200 ${selected ? "border-[#1768f2] bg-[#f5f8ff]" : "border-[#dbe4f1] bg-white hover:border-[#8eafe9] hover:bg-[#fbfdff]"}`}>
                  <span className="flex items-start gap-4">
                    <input
                      type="radio"
                      name="revocation-reason"
                      checked={selected}
                      onChange={() => onReasonChange(option.code)}
                      className="mt-1 size-5 cursor-pointer accent-[#1768f2] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df]"
                    />
                    <span>
                      <strong className="block text-[#061a50]">{option.title}</strong>
                      <span className="mt-1 block text-sm leading-5 text-[#52678f]">{option.description}</span>
                    </span>
                  </span>
                  {option.code === "OTHER" && selected ? (
                    <span className="mt-4 block border-t border-[#e3e9f2] pt-4 pl-9">
                      <span className="mb-2 block text-sm font-bold text-[#172b59]">Describe brevemente el motivo</span>
                      <textarea
                        value={otherReason}
                        onChange={event => onOtherReasonChange(event.target.value)}
                        minLength={10}
                        maxLength={300}
                        rows={4}
                        className="w-full resize-y rounded-lg border border-[#aebdd3] bg-white px-4 py-3 text-[#172b59] outline-none transition-colors focus:border-[#0755df] focus:ring-2 focus:ring-[#dce9ff]"
                        aria-describedby="other-reason-help"
                      />
                      <span id="other-reason-help" className="mt-1 flex justify-between gap-3 text-xs text-[#65779b]">
                        <span>Mínimo 10 caracteres.</span>
                        <span>{otherReason.length}/300</span>
                      </span>
                    </span>
                  ) : null}
                </label>
              );
            })}
          </fieldset>

          <div className="mt-6 flex flex-col-reverse justify-between gap-3 border-t border-[#e1e8f2] pt-5 sm:flex-row">
            <FlowNavigationButton variant="secondary" onClick={onBack}>
              <BackIcon /> Regresar
            </FlowNavigationButton>
            <FlowNavigationButton variant="primary" onClick={onContinue} disabled={!canContinue}>
              Continuar <ArrowIcon />
            </FlowNavigationButton>
          </div>
        </FlowStepContent>
      </div>
    </section>
  );
}

function BackIcon() {
  return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M19 12H5m5-5-5 5 5 5" /></svg>;
}

function ArrowIcon() {
  return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5" /></svg>;
}
