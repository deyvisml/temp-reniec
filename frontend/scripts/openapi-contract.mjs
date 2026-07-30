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
  ? JSON.parse((await readFile(resolve(process.env.OPENAPI_SCHEMA_FILE), "utf8")).replace(/^\uFEFF/, ""))
  : await fetchSchema(openApiUrl);
const contract = structuredClone(schema);
delete contract.servers;
assertInitialResponseBoundary(contract);
assertRecaptchaRequestBoundary(contract);
assertSingleCertificateBoundary(contract);
const snapshot = `${JSON.stringify(sortRecursively(contract), null, 2)}\n`;
const generated = astToString(await openapiTS(contract));

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

function assertInitialResponseBoundary(document) {
  const response = document.components?.schemas?.CancellationRequestResponse;
  const properties = response?.properties ?? {};
  const required = new Set(response?.required ?? []);
  const forbidden = [
    "eligibilityResult",
    "certificates",
    "certificateCount",
    "orderNumber",
    "emissionCreatedAt",
    "certificateUuid",
    "uuid",
  ];

  if (!("availabilityResult" in properties) || !required.has("availabilityResult")) {
    throw new Error("El contrato inicial debe exigir availabilityResult.");
  }
  const leaked = forbidden.filter((property) => property in properties);
  if (leaked.length > 0) {
    throw new Error(`La respuesta inicial expone propiedades no permitidas: ${leaked.join(", ")}.`);
  }
}

function assertRecaptchaRequestBoundary(document) {
  const request = document.components?.schemas?.StartCancellationRequest;
  const properties = request?.properties ?? {};
  const required = new Set(request?.required ?? []);
  const token = properties.recaptchaToken;
  if (!token || !required.has("recaptchaToken") || token.writeOnly !== true || token.maxLength !== 4096) {
    throw new Error("El contrato inicial debe exigir recaptchaToken efímero, writeOnly y acotado.");
  }
  const serialized = JSON.stringify(document);
  for (const forbidden of ["RECAPTCHA_SECRET_KEY", "test-recaptcha-valid", "recaptchaSecret", "secretKey"]) {
    if (serialized.includes(forbidden)) {
      throw new Error(`El contrato OpenAPI expone configuración o evidencia prohibida: ${forbidden}.`);
    }
  }
}

function assertSingleCertificateBoundary(document) {
  const selection = document.components?.schemas?.CertificateSelectionRequest;
  const selectionProperties = selection?.properties ?? {};
  const selectionRequired = new Set(selection?.required ?? []);
  if (!("certificateUuid" in selectionProperties) || !selectionRequired.has("certificateUuid")) {
    throw new Error("El contrato de selección debe exigir un certificateUuid singular.");
  }
  if ("certificateUuids" in selectionProperties || selectionProperties.certificateUuid?.type === "array") {
    throw new Error("El contrato de selección no debe aceptar colecciones de certificados.");
  }

  const review = document.components?.schemas?.CancellationReviewResponse;
  const reviewProperties = review?.properties ?? {};
  const reviewRequired = new Set(review?.required ?? []);
  if (!("certificate" in reviewProperties) || !reviewRequired.has("certificate")
      || "certificates" in reviewProperties) {
    throw new Error("El resumen de confirmación debe exponer un certificate singular.");
  }
}
