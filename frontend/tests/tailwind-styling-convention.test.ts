import { readFileSync, readdirSync } from "node:fs";
import { join, relative } from "node:path";
import { describe, expect, it } from "vitest";

const frontendRoot = process.cwd();

function filesBelow(directory: string, extension: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) return filesBelow(path, extension);
    return entry.isFile() && entry.name.endsWith(extension) ? [path] : [];
  });
}

describe("Tailwind-first styling convention", () => {
  it("keeps the global stylesheet limited to Tailwind and global theme concerns", () => {
    const appDirectory = join(frontendRoot, "app");
    const cssFiles = filesBelow(appDirectory, ".css");
    const relativeCssFiles = cssFiles.map((file) => relative(appDirectory, file));
    const globalCss = readFileSync(join(appDirectory, "globals.css"), "utf8");

    expect(relativeCssFiles).toEqual(["globals.css"]);
    expect(globalCss).toContain('@import "tailwindcss"');
    expect(globalCss).not.toMatch(/@apply\b/);
    expect(globalCss).not.toMatch(/(^|})\s*\.[a-z_][\w-]*/m);
  });

  it("does not use visual style props in application components", () => {
    const sourceFiles = [
      ...filesBelow(join(frontendRoot, "app"), ".tsx"),
      ...filesBelow(join(frontendRoot, "components"), ".tsx"),
    ];

    for (const sourceFile of sourceFiles) {
      const source = readFileSync(sourceFile, "utf8");
      expect(source, relative(frontendRoot, sourceFile)).not.toMatch(/\bstyle\s*=\s*\{\{/);
    }
  });

  it("keeps focus indicators compact across the application", () => {
    const sourceFiles = [
      ...filesBelow(join(frontendRoot, "app"), ".tsx"),
      ...filesBelow(join(frontendRoot, "components"), ".tsx"),
    ];

    for (const sourceFile of sourceFiles) {
      const source = readFileSync(sourceFile, "utf8");
      expect(source, relative(frontendRoot, sourceFile)).not.toMatch(/(?:focus|focus-visible|focus-within):outline-3\b/);
      expect(source, relative(frontendRoot, sourceFile)).not.toMatch(/(?:focus|focus-visible|focus-within):outline-offset-3\b/);
      expect(source, relative(frontendRoot, sourceFile)).not.toMatch(/focus-within:ring-3\b/);
    }
  });

  it("keeps the citizen home presentation colocated as Tailwind utilities", () => {
    const homeSource = readFileSync(join(frontendRoot, "components", "cancellation-entry.tsx"), "utf8");
    const formSource = readFileSync(join(frontendRoot, "components", "dni-availability-form.tsx"), "utf8");
    const outcomeAlertSource = readFileSync(
      join(frontendRoot, "components", "availability-outcome-alert.tsx"),
      "utf8",
    );
    const layoutSource = readFileSync(join(frontendRoot, "app", "layout.tsx"), "utf8");

    expect(homeSource).toContain("max-w-[920px]");
    expect(homeSource).toContain("max-[800px]:grid-cols-1");
    expect(formSource).toContain("focus-within:border-[#0755df]");
    expect(formSource).toContain("motion-reduce:animate-none");
    expect(outcomeAlertSource).toContain("buttonsStyling: false");
    expect(outcomeAlertSource).toContain("motion-reduce:transition-none");
    expect(outcomeAlertSource).not.toMatch(/\.swal2-/);
    expect(layoutSource).toContain('import "sweetalert2/dist/sweetalert2.min.css"');
  });
});
