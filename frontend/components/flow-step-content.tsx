import type { HTMLAttributes } from "react";

export function FlowStepContent({
  className = "",
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`mx-auto w-full max-w-[720px] ${className}`.trim()}
      {...props}
    />
  );
}
