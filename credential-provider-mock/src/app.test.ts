import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { resolve } from "node:path";

import type { FastifyInstance } from "fastify";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { buildApp } from "./app.js";
import type { AppConfig } from "./types.js";

const API_KEY = "integration-test-key";
const SEED_FILE = resolve("fixtures/credentials.seed.json");

describe("credential provider HTTP contracts", () => {
  let directory: string;
  let dataFile: string;
  let app: FastifyInstance;

  beforeEach(async () => {
    directory = await mkdtemp(resolve(tmpdir(), "credential-provider-"));
    dataFile = resolve(directory, "credentials.json");
    app = await buildApp(config(dataFile));
  });

  afterEach(async () => {
    await app.close();
    await rm(directory, { recursive: true, force: true });
  });

  it("protects every POST and validates exact JSON bodies", async () => {
    const missingKey = await app.inject({
      method: "POST",
      url: "/api/v1/has-credentials",
      payload: { dni: "00000001" },
    });
    expect(missingKey.statusCode).toBe(401);

    const invalidKey = await app.inject({
      method: "POST",
      url: "/api/v1/has-credentials",
      headers: { "x-api-key": "incorrect-key" },
      payload: { dni: "00000001" },
    });
    expect(invalidKey.statusCode).toBe(401);

    const additionalField = await post("/api/v1/has-credentials", { dni: "00000001", extra: true });
    expect(additionalField.statusCode).toBe(400);

    const invalidDni = await post("/api/v1/list-credentials", { dni: "123" });
    expect(invalidDni.statusCode).toBe(400);

    const wrongContentType = await app.inject({
      method: "POST",
      url: "/api/v1/has-credentials",
      headers: { "content-type": "text/plain", "x-api-key": API_KEY },
      payload: '{"dni":"00000001"}',
    });
    expect(wrongContentType.statusCode).toBe(415);
  });

  it("reports availability and returns official ordered credential shapes", async () => {
    const available = await post("/api/v1/has-credentials", { dni: "00000001" });
    expect(available.json()).toEqual({ title: "Adapter Reniec", credentials: true });

    const empty = await post("/api/v1/has-credentials", { dni: "00000020" });
    expect(empty.json()).toEqual({ title: "Adapter Reniec", credentials: false });

    const onlyRevoked = await post("/api/v1/has-credentials", { dni: "00000028" });
    expect(onlyRevoked.json().credentials).toBe(false);

    const listing = await post("/api/v1/list-credentials", { dni: "00000001" });
    expect(listing.statusCode).toBe(200);
    expect(listing.json()).toEqual([
      expect.objectContaining({
        title: "Adapter Reniec",
        listCredential: "11111111-1111-4111-8111-111111111111",
        statusListIndex: 14,
        credentialStatus: 0,
        revocateDate: null,
      }),
      expect.objectContaining({
        listCredential: "11111111-1111-4111-8111-111111111111",
        statusListIndex: 31,
        credentialStatus: 0,
      }),
      expect.objectContaining({ statusListIndex: 44, credentialStatus: 1 }),
    ]);

    const unknown = await post("/api/v1/list-credentials", { dni: "99999999" });
    expect(unknown.json()).toEqual([]);
  });

  it("accepts repeated UUIDs when each credential has a distinct index", async () => {
    const listing = await post("/api/v1/list-credentials", { dni: "00000001" });
    const credentials = listing.json<Array<{ listCredential: string; statusListIndex: number }>>();

    expect(credentials.slice(0, 2)).toEqual([
      expect.objectContaining({
        listCredential: "11111111-1111-4111-8111-111111111111",
        statusListIndex: 14,
      }),
      expect.objectContaining({
        listCredential: "11111111-1111-4111-8111-111111111111",
        statusListIndex: 31,
      }),
    ]);
  });

  it("revokes the exact tuple idempotently and rejects mismatches", async () => {
    const body = {
      listCredential: "44444444-4444-4444-8444-444444444444",
      statusListIndex: 21,
      cui_dni: "00000021",
    };
    const first = await post("/api/v1/revocation", body);
    expect(first.statusCode).toBe(200);
    expect(first.json()).toEqual({
      title: "Adapter Reniec",
      message: "La credencial fue revocada",
      credentialStatus: 1,
    });

    const repeated = await post("/api/v1/revocation", body);
    expect(repeated.statusCode).toBe(200);
    expect(repeated.json().message).toBe("La credencial ya fue revocada");

    for (const mismatch of [
      { ...body, statusListIndex: 999 },
      { ...body, cui_dni: "00000001" },
      { ...body, listCredential: "99999999-9999-4999-8999-999999999999" },
    ]) {
      const response = await post("/api/v1/revocation", mismatch);
      expect(response.statusCode).toBe(404);
    }

    const persisted = JSON.parse(await readFile(dataFile, "utf8")) as {
      citizens: Record<string, Array<{ credentialStatus: number; revocateDate: string | null }>>;
    };
    expect(persisted.citizens["00000021"]?.[0]?.credentialStatus).toBe(1);
    expect(persisted.citizens["00000021"]?.[0]?.revocateDate).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/,
    );
  });

  it("serves the fixed personal DNI and restores it from the seed fixture", async () => {
    const personal = await post("/api/v1/list-credentials", { dni: "73905791" });
    expect(personal.json()).toHaveLength(3);
    expect(personal.body).not.toContain("73905791");

    await post("/api/v1/revocation", {
      listCredential: "a1111111-1111-4111-8111-111111111111",
      statusListIndex: 41,
      cui_dni: "73905791",
    });
    const reset = await post("/__admin/reset", {});
    expect(reset.json()).toEqual({ reset: true });

    const restored = await post("/api/v1/list-credentials", { dni: "73905791" });
    expect(restored.json()[0]).toMatchObject({ credentialStatus: 0, revocateDate: null });
  });

  it("serves four active credentials and one revoked credential for the second fixed DNI", async () => {
    const response = await post("/api/v1/list-credentials", { dni: "42992664" });
    const credentials = response.json<Array<{ credentialStatus: number; statusListIndex: number }>>();

    expect(credentials).toHaveLength(5);
    expect(credentials.filter((credential) => credential.credentialStatus === 0)).toHaveLength(4);
    expect(credentials.filter((credential) => credential.credentialStatus === 1)).toHaveLength(1);
    expect(credentials.map((credential) => credential.statusListIndex)).toEqual([51, 52, 53, 54, 55]);
    expect(response.body).not.toContain("42992664");
  });

  function post(url: string, payload: unknown) {
    return app.inject({
      method: "POST",
      url,
      headers: { "content-type": "application/json", "x-api-key": API_KEY },
      payload,
    });
  }
});

function config(dataFile: string): AppConfig {
  return {
    host: "127.0.0.1",
    port: 8081,
    apiKey: API_KEY,
    seedFile: SEED_FILE,
    dataFile,
  };
}
