import { readFile, writeFile, mkdir } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import process from "node:process";

import openapiTS, { astToString } from "openapi-typescript";

const mode = process.argv[2];
if (mode !== "sync" && mode !== "check") {
  throw new Error("Uso: node scripts/openapi-contract.mjs <sync|check>");
}

const root = process.cwd();
const snapshotPath = resolve(root, "openapi/backend-api.json");
const typesPath = resolve(root, "lib/api/generated.ts");
const backendUrl = (process.env.BACKEND_URL || "http://localhost:8080").replace(/\/$/, "");
const openApiUrl = process.env.BACKEND_OPENAPI_URL || `${backendUrl}/v3/api-docs`;

const schema = process.env.OPENAPI_SCHEMA_FILE
  ? JSON.parse(await readFile(resolve(process.env.OPENAPI_SCHEMA_FILE), "utf8"))
  : await fetchSchema(openApiUrl);
const snapshot = `${JSON.stringify(sortRecursively(schema), null, 2)}\n`;
const generated = astToString(await openapiTS(schema));

if (mode === "sync") {
  await Promise.all([mkdir(dirname(snapshotPath), { recursive: true }), mkdir(dirname(typesPath), { recursive: true })]);
  await Promise.all([writeFile(snapshotPath, snapshot, "utf8"), writeFile(typesPath, generated, "utf8")]);
  console.log("Contrato OpenAPI y tipos TypeScript sincronizados.");
} else {
  const [committedSnapshot, committedTypes] = await Promise.all([
    readRequired(snapshotPath),
    readRequired(typesPath),
  ]);
  if (committedSnapshot !== snapshot || committedTypes !== generated) {
    throw new Error("El contrato generado está desalineado. Ejecuta npm run api:sync y revisa los cambios.");
  }
  console.log("Contrato OpenAPI y tipos TypeScript alineados.");
}

function sortRecursively(value) {
  if (Array.isArray(value)) return value.map(sortRecursively);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, nested]) => [key, sortRecursively(nested)]),
    );
  }
  return value;
}

async function readRequired(path) {
  try {
    return await readFile(path, "utf8");
  } catch {
    throw new Error("Faltan artefactos generados. Ejecuta npm run api:sync.");
  }
}

async function fetchSchema(url) {
  let response;
  try {
    response = await fetch(url, { headers: { Accept: "application/json" } });
  } catch {
    throw new Error(`No se pudo consultar OpenAPI en ${url}. Inicia el backend local.`);
  }
  if (!response.ok) throw new Error(`OpenAPI respondió HTTP ${response.status} en ${url}.`);
  return response.json();
}
