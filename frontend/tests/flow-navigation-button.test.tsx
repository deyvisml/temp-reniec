import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { FlowNavigationButton } from "@/components/flow-navigation-button";

describe("botones de navegación del flujo", () => {
  it("comparte dimensiones y estados entre acciones primarias y secundarias", () => {
    const markup = renderToStaticMarkup(
      <div>
        <FlowNavigationButton variant="secondary">Regresar</FlowNavigationButton>
        <FlowNavigationButton variant="primary" disabled>Continuar</FlowNavigationButton>
      </div>,
    );

    expect(markup.match(/min-h-12/g)?.length).toBe(2);
    expect(markup.match(/w-full/g)?.length).toBe(2);
    expect(markup.match(/sm:w-\[280px\]/g)?.length).toBe(2);
    expect(markup.match(/rounded-lg/g)?.length).toBe(2);
    expect(markup.match(/focus-visible:outline-\[#0755df\]/g)?.length).toBe(2);
    expect(markup).toContain("hover:not-disabled:bg-[#a8003f]");
    expect(markup).toContain("hover:not-disabled:bg-[#edf4ff]");
    expect(markup).toContain("active:not-disabled:bg-[#920038]");
    expect(markup).toContain("active:not-disabled:bg-[#dfeaff]");
    expect(markup).toContain("disabled");
    expect(markup).toContain("disabled:cursor-not-allowed");
  });
});
