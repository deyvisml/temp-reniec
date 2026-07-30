import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { CancellationReasonTransition } from "@/components/cancellation-reason-transition";

describe("motivo de cancelación", () => {
  it("renderiza el paso funcional y no la transición provisional", () => {
    const markup = renderToStaticMarkup(
      <CancellationReasonTransition onBack={() => undefined} onContinue={() => undefined} />,
    );

    expect(markup).toContain("Cargando el motivo");
    expect(markup).not.toContain("Selección guardada");
  });

  it("usa los mismos estados azules de interacción que el paso de certificados", () => {
    const source = readFileSync(resolve(process.cwd(), "components/cancellation-reason-transition.tsx"), "utf8");

    expect(source).toContain('hover:border-[#8eafe9] hover:bg-[#fbfdff]');
    expect(source).toContain('border-[#1768f2] bg-[#f5f8ff]');
    expect(source).toContain('accent-[#1768f2]');
    expect(source).not.toContain('"border-reniec-red"');
    expect(source).not.toContain("accent-reniec-red");
  });
});
