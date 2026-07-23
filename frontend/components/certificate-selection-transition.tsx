import { CancellationStepper } from "@/components/cancellation-stepper";

export function CertificateSelectionTransition() {
  return (
    <section className="mx-auto w-full max-w-[1040px]" aria-labelledby="selection-title">
      <div className="px-2 sm:px-8 lg:px-14">
        <CancellationStepper currentStep={2} />
      </div>
      <div className="mx-2 mt-6 rounded-[22px] border border-[#dfe7f3] bg-white/95 px-6 py-12 text-center shadow-[0_24px_70px_-36px_#001b6066] sm:mx-8 sm:px-10 lg:mx-14">
        <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">
          PASO 2 DE 5
        </p>
        <h1 id="selection-title" className="mt-4 text-3xl font-black tracking-[-0.025em] text-[#061a50] sm:text-4xl">
          Selección de certificados
        </h1>
        <p className="mx-auto mt-4 max-w-[560px] text-pretty leading-7 text-[#52678f]">
          Tu identidad fue verificada correctamente.
        </p>
      </div>
    </section>
  );
}
