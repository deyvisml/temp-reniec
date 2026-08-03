import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { FlowStepContent } from "@/components/flow-step-content";

describe("ancho del contenido de los pasos", () => {
  it("limita el contenido a 720 px sin impedir que se adapte en móvil", () => {
    const markup = renderToStaticMarkup(
      <FlowStepContent>Contenido</FlowStepContent>,
    );

    expect(markup).toContain("w-full");
    expect(markup).toContain("max-w-[720px]");
    expect(markup).toContain("mx-auto");
  });

  it.each([
    "digital-credential-selection-transition.tsx",
    "revocation-reason-transition.tsx",
    "revocation-review-views.tsx",
    "revocation-receipt-transition.tsx",
  ])("usa el contenedor compartido en %s", (fileName) => {
    const source = readFileSync(
      resolve(process.cwd(), "components", fileName),
      "utf8",
    );

    expect(source).toContain("<FlowStepContent");
  });

  it("no aplica el contenedor compartido al paso de autenticación", () => {
    const source = readFileSync(
      resolve(process.cwd(), "components", "identity-verification-panel.tsx"),
      "utf8",
    );

    expect(source).not.toContain("FlowStepContent");
  });
});
