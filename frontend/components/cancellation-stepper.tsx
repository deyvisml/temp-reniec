const labels = ["Autenticación", "Selección", "Motivo", "Confirmación", "Constancia"];

export function CancellationStepper({ currentStep }: { currentStep: number }) {
  return (
    <ol className="grid grid-cols-5" aria-label="Progreso del proceso">
      {labels.map((label, index) => {
        const step = index + 1;
        const isCurrent = step === currentStep;
        const isComplete = step < currentStep;

        return (
          <li className="relative min-w-0" key={label}>
            {index < labels.length - 1 ? (
              <span
                className={`absolute left-[calc(50%+1.1rem)] right-[calc(-50%+1.1rem)] top-4 h-px sm:left-[calc(50%+1.4rem)] sm:right-[calc(-50%+1.4rem)] sm:top-5 ${isComplete ? "bg-reniec-red" : "bg-[#d4deed]"}`}
                aria-hidden="true"
              />
            ) : null}
            <div
              className={`relative z-1 flex w-full flex-col items-center gap-2 text-center text-[10px] font-bold sm:text-xs ${isCurrent ? "text-reniec-red" : "text-[#7583a4]"}`}
              aria-current={isCurrent ? "step" : undefined}
              aria-label={`${isCurrent ? "Paso actual" : isComplete ? "Paso completado" : "Paso pendiente"} ${step}: ${label}`}
            >
              <span className={`grid size-8 place-items-center rounded-full border-2 sm:size-10 ${isCurrent ? "border-reniec-red bg-reniec-red text-white" : isComplete ? "border-reniec-red bg-white text-reniec-red" : "border-[#d4deed] bg-white"}`}>
                {step}
              </span>
              <span className="max-[420px]:text-[9px]">{label}</span>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
