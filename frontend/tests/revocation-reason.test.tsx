import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { RevocationReasonTransition } from "@/components/revocation-reason-transition";

describe("motivo de revocación", () => {
  it("renderiza el paso funcional y no la transición provisional", () => {
    const markup = renderToStaticMarkup(
      <RevocationReasonTransition
        reason={null}
        otherReason=""
        onReasonChange={() => undefined}
        onOtherReasonChange={() => undefined}
        onBack={() => undefined}
        onContinue={() => undefined}
      />,
    );

    expect(markup).toContain("PASO 3 DE 5");
    expect(markup).not.toContain("Selección guardada");
    expect(markup.match(/sm:w-\[280px\]/g)?.length).toBe(2);
  });

  it("usa los mismos estados azules de interacción que el paso de credenciales", () => {
    const source = readFileSync(resolve(process.cwd(), "components/revocation-reason-transition.tsx"), "utf8");

    expect(source).toContain('hover:border-[#8eafe9] hover:bg-[#fbfdff]');
    expect(source).toContain('border-[#1768f2] bg-[#f5f8ff]');
    expect(source).toContain('accent-[#1768f2]');
    expect(source).not.toContain('"border-reniec-red"');
    expect(source).not.toContain("accent-reniec-red");
  });

  it("mantiene el motivo como estado controlado sin persistencia previa", () => {
    const source = readFileSync(resolve(process.cwd(), "components/revocation-reason-transition.tsx"), "utf8");
    expect(source).not.toMatch(/fetch|saveCurrentRevocationReason|getCurrentRevocationReason|localStorage|sessionStorage/);
    expect(source).toContain("onReasonChange");
    expect(source).toContain("onOtherReasonChange");
    expect(source).not.toContain("Guardando motivo");
  });
});
