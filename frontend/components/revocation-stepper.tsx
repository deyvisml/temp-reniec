const labels = ["Autenticación", "Selección", "Motivo", "Confirmación", "Constancia"];

export function RevocationStepper({ currentStep, navigableSteps = [], onNavigate }: {
  currentStep: number;
  navigableSteps?: readonly number[];
  onNavigate?: (step: number) => void;
}) {
  return (
    <ol className="grid grid-cols-5" aria-label="Progreso del proceso">
      {labels.map((label, index) => {
        const step = index + 1;
        const isCurrent = step === currentStep;
        const isComplete = step < currentStep;
        const isAccessible = navigableSteps.includes(step) && !isCurrent && Boolean(onNavigate);

        return (
          <li className="relative min-w-0" key={label}>
            {index < labels.length - 1 ? (
              <span
                className={`absolute left-[calc(50%+1.1rem)] right-[calc(-50%+1.1rem)] top-4 h-px sm:left-[calc(50%+1.4rem)] sm:right-[calc(-50%+1.4rem)] sm:top-5 ${isComplete ? "bg-reniec-red" : "bg-[#d4deed]"}`}
                aria-hidden="true"
              />
            ) : null}
            <button
              type="button"
              disabled={!isAccessible}
              onClick={() => onNavigate?.(step)}
              className={`group relative z-1 flex w-full flex-col items-center gap-2 bg-transparent text-center text-[10px] font-bold sm:text-xs ${isCurrent || isComplete ? "text-reniec-red" : "text-[#7583a4]"} ${isAccessible ? "cursor-pointer" : "cursor-default"}`}
              aria-current={isCurrent ? "step" : undefined}
              aria-label={`${isCurrent ? "Paso actual" : isComplete ? "Paso completado" : "Paso pendiente"} ${step}: ${label}`}
            >
              <span className={`grid size-8 place-items-center rounded-full border-2 transition-colors sm:size-10 ${isCurrent ? "border-reniec-red bg-reniec-red text-white ring-4 ring-[#fae9f0]" : isComplete ? "border-reniec-red bg-[#fff4f7] text-reniec-red group-hover:bg-[#fae9f0] group-focus-visible:bg-[#fae9f0]" : "border-[#d4deed] bg-white"}`}>
                {isComplete ? <CheckIcon /> : step}
              </span>
              <span className="max-[420px]:text-[9px]">{label}</span>
            </button>
          </li>
        );
      })}
    </ol>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-4 fill-none stroke-current stroke-[2.5] sm:size-5" aria-hidden="true">
      <path d="m6.5 12.5 3.5 3.5 7.5-8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
