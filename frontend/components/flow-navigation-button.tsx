import type { ButtonHTMLAttributes } from "react";

const baseClasses =
  "inline-flex min-h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-lg px-6 font-bold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df] disabled:cursor-not-allowed sm:w-[280px]";

const variantClasses = {
  primary:
    "bg-reniec-red text-white hover:not-disabled:bg-[#a8003f] active:not-disabled:bg-[#920038] disabled:bg-[#c9cfdb]",
  secondary:
    "bg-transparent text-[#0755df] hover:not-disabled:bg-[#edf4ff] active:not-disabled:bg-[#dfeaff] disabled:opacity-60",
} as const;

export function FlowNavigationButton({
  variant,
  className = "",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant: keyof typeof variantClasses;
}) {
  return (
    <button
      type="button"
      className={`${baseClasses} ${variantClasses[variant]} ${className}`.trim()}
      {...props}
    />
  );
}
