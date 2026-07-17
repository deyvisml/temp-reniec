import { spawn } from "node:child_process";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { resolve } from "node:path";

import { describe, expect, it } from "vitest";

describe("OpenAPI drift check", () => {
  it("fails when committed snapshot or generated types are misaligned", async () => {
    const temporaryRoot = await mkdtemp(resolve(tmpdir(), "cancelacion-openapi-"));
    try {
      await mkdir(resolve(temporaryRoot, "openapi"), { recursive: true });
      await mkdir(resolve(temporaryRoot, "lib/api"), { recursive: true });
      await writeFile(resolve(temporaryRoot, "openapi/backend-api.json"), "{}\n", "utf8");
      await writeFile(resolve(temporaryRoot, "lib/api/generated.ts"), "export {};\n", "utf8");

      const result = await runNode(
        resolve(process.cwd(), "scripts/openapi-contract.mjs"),
        temporaryRoot,
        resolve(process.cwd(), "openapi/backend-api.json"),
      );

      expect(result.code).not.toBe(0);
      expect(result.output).toContain("desalineado");
    } finally {
      await rm(temporaryRoot, { recursive: true, force: true });
    }
  });
});

function runNode(script: string, cwd: string, schemaFile: string) {
  return new Promise<{ code: number | null; output: string }>((resolveResult) => {
    const child = spawn(process.execPath, [script, "check"], {
      cwd,
      env: { ...process.env, OPENAPI_SCHEMA_FILE: schemaFile },
      windowsHide: true,
    });
    let output = "";
    child.stdout.on("data", (chunk) => { output += String(chunk); });
    child.stderr.on("data", (chunk) => { output += String(chunk); });
    child.on("close", (code) => resolveResult({ code, output }));
  });
}
