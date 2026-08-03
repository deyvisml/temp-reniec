import { timingSafeEqual } from "node:crypto";

import Fastify, { type FastifyInstance } from "fastify";

import { CredentialStore } from "./store.js";
import type { AppConfig, Credential } from "./types.js";

const DNI_PATTERN = "^[0-9]{8}$";
const UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";
const TITLE = "Adapter Reniec";

const dniBodySchema = {
  type: "object",
  additionalProperties: false,
  required: ["dni"],
  properties: { dni: { type: "string", pattern: DNI_PATTERN } },
} as const;

const revocationBodySchema = {
  type: "object",
  additionalProperties: false,
  required: ["listCredential", "statusListIndex", "cui_dni"],
  properties: {
    listCredential: { type: "string", pattern: UUID_PATTERN },
    statusListIndex: { type: "integer", minimum: 0 },
    cui_dni: { type: "string", pattern: DNI_PATTERN },
  },
} as const;

interface DniBody { dni: string }
interface RevocationBody { listCredential: string; statusListIndex: number; cui_dni: string }

export async function buildApp(config: AppConfig): Promise<FastifyInstance> {
  const app = Fastify({
    logger: false,
    ajv: { customOptions: { removeAdditional: false } },
  });
  const store = new CredentialStore(
    config.seedFile,
    config.dataFile,
    config.personalTestDni,
    config.additionalTestDni,
  );
  await store.initialize();

  app.addHook("onRequest", async (request, reply) => {
    if (request.method !== "POST") return;
    const contentType = request.headers["content-type"]?.split(";", 1)[0]?.trim().toLowerCase();
    if (contentType !== "application/json") {
      return reply.code(415).send({ title: TITLE, message: "Se requiere application/json" });
    }
    const supplied = request.headers["x-api-key"];
    if (typeof supplied !== "string" || !sameSecret(supplied, config.apiKey)) {
      return reply.code(401).send({ title: TITLE, message: "API key inválida" });
    }
  });

  app.get("/health", async () => ({ status: "UP" }));

  app.post<{ Body: DniBody }>("/api/v1/has-credentials", { schema: { body: dniBodySchema } }, async (request) => ({
    title: TITLE,
    credentials: store.hasActiveCredentials(request.body.dni),
  }));

  app.post<{ Body: DniBody }>("/api/v1/list-credentials", { schema: { body: dniBodySchema } }, async (request) => (
    store.listCredentials(request.body.dni).map(providerCredential)
  ));

  app.post<{ Body: RevocationBody }>("/api/v1/revocation", {
    schema: { body: revocationBodySchema },
  }, async (request, reply) => {
    const result = await store.revoke(
      request.body.cui_dni,
      request.body.listCredential,
      request.body.statusListIndex,
    );
    if (!result.found) {
      return reply.code(404).send({ title: TITLE, message: "Credencial no encontrada" });
    }
    return {
      title: TITLE,
      message: result.alreadyRevoked ? "La credencial ya fue revocada" : "La credencial fue revocada",
      credentialStatus: result.credentialStatus,
    };
  });

  app.post("/__admin/reset", async () => {
    await store.reset();
    return { reset: true };
  });

  return app;
}

function providerCredential(credential: Credential): Credential & { title: string } {
  return { title: TITLE, ...credential };
}

function sameSecret(candidate: string, expected: string): boolean {
  const candidateBytes = Buffer.from(candidate);
  const expectedBytes = Buffer.from(expected);
  return candidateBytes.length === expectedBytes.length && timingSafeEqual(candidateBytes, expectedBytes);
}
