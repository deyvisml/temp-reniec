import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { describe, expect, it } from "vitest";

describe("estilos globales de interacción", () => {
  it("comunica visualmente cuándo un botón es interactivo", () => {
    const styles = readFileSync(resolve(process.cwd(), "app/globals.css"), "utf8");

    expect(styles).toMatch(/button:enabled\s*\{[\s\S]*?cursor:\s*pointer/);
    expect(styles).toMatch(/button:disabled\s*\{[\s\S]*?cursor:\s*not-allowed/);
  });
});
